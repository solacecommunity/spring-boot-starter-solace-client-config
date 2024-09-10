package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import static com.solacesystems.jcsmp.JCSMPProperties.*;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.*;

/**
 * A bean post processor used to enhance the JCSMP configuration properties with authentication objects created from the PEM format.
 */
final class JCSMPPropertiesPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(JCSMPPropertiesPostProcessor.class);

    private final KeyStoreFactory keyStoreFactory;
    private TaskScheduler taskScheduler;
    private SslCertInfoProperties sslCertInfoProperties;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        try {
            taskScheduler = beanFactory.getBean(TaskScheduler.class);
        } catch (Exception ex) {
            taskScheduler = null;
        }
        sslCertInfoProperties = beanFactory.getBean(SslCertInfoProperties.class);
    }

    public JCSMPPropertiesPostProcessor(KeyStoreFactory keyStoreFactory) {
        this.keyStoreFactory = keyStoreFactory;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (sslCertInfoProperties == null) {
            return bean;
        }
        if (bean instanceof JCSMPProperties) {
            LOG.debug("Postprocessing {} bean", bean.getClass());
            return addAuthenticationProperties(((JCSMPProperties) bean));
        }
        return bean;
    }

    private JCSMPProperties addAuthenticationProperties(final JCSMPProperties jcsmpProperties) {
        if (!AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE.equals(jcsmpProperties.getStringProperty(AUTHENTICATION_SCHEME))) {
            LOG.debug("AUTHENTICATION_SCHEME is not set to AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE, skip...");
            return jcsmpProperties;
        }
        if (!StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_PRIVATE_KEY))) {
            LOG.warn("No SSL_PRIVATE_KEY present, skip...");
            return jcsmpProperties;
        }
        if (!StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_CLIENT_CERT))) {
            LOG.warn("No SSL_CLIENT_CERT present, skip...");
            return jcsmpProperties;
        }

        LOG.debug("Adding Solace ClientCert properties to JSCMPProperties");

        KeyStore keyStore = keyStoreFactory.createClientKeyStore(jcsmpProperties.getStringProperty(SSL_PRIVATE_KEY), jcsmpProperties.getStringProperty(SSL_CLIENT_CERT));

        if (keyStore == null) {
            LOG.warn("No keyStore was created, skip...");
            return jcsmpProperties;
        }
        checkValidToPeriodically(keyStoreFactory.getValidTo(jcsmpProperties.getStringProperty(SSL_CLIENT_CERT)));
        jcsmpProperties.setProperty(SSL_IN_MEMORY_KEY_STORE, keyStore);
        jcsmpProperties.setProperty(SSL_KEY_STORE_PASSWORD, keyStoreFactory.getClientKeyStorePassword());

        // Solace config is struggling with empty strings.
        if (jcsmpProperties.getProperty("SSL_KEY_STORE") instanceof String && !StringUtils.hasText((CharSequence) jcsmpProperties.getProperty("SSL_KEY_STORE"))) {
            jcsmpProperties.setProperty("SSL_KEY_STORE", "");
            jcsmpProperties.setProperty(SSL_KEY_STORE_PASSWORD, KeyStoreFactory.INTERNAL_PASSWORD);
            unsetProperty(jcsmpProperties, "SSL_PRIVATE_KEY_PASSWORD");
            unsetProperty(jcsmpProperties, "SSL_PRIVATE_KEY_ALIAS");
        }

        if (!StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_TRUST_CERT))) {
            LOG.debug("No SSL_TRUST_CERT present, dont add SSL_IN_MEMORY_TRUST_STORE");
            return jcsmpProperties;
        }
        LOG.debug("Adding Solace TrustStore properties to JSCMPProperties");
        KeyStore trustStore = keyStoreFactory.createTrustStore(jcsmpProperties.getStringProperty(SSL_TRUST_CERT));
        jcsmpProperties.setProperty(SSL_IN_MEMORY_TRUST_STORE, trustStore);
        if (jcsmpProperties.getProperty("SSL_TRUST_STORE") instanceof String && !StringUtils.hasText((CharSequence) jcsmpProperties.getProperty("SSL_TRUST_STORE"))) {
            jcsmpProperties.setProperty("SSL_TRUST_STORE", "");
            unsetProperty(jcsmpProperties, "SSL_TRUST_STORE_PASSWORD");
            unsetTrustStore(jcsmpProperties);
        }
        return jcsmpProperties;
    }

    private void checkValidToPeriodically(Instant notAfter) {
        if (notAfter == null || !sslCertInfoProperties.isEnabled()) {
            return;
        }
        if (taskScheduler == null) {
            LOG.warn("Cant verify certificate expiration because taskScheduler is missing");
            return;
        }

        // Always log at 09:00AM to not trigger nightly support on error.
        Instant workDayBegin = Instant.now().atZone(ZoneId.systemDefault()).withHour(9).withMinute(0).withSecond(0).toInstant();
        if (Instant.now().isAfter(workDayBegin)) {
            workDayBegin = workDayBegin.plus(1, ChronoUnit.DAYS);
        }

        taskScheduler.scheduleAtFixedRate(() -> {
            long validForDays = Duration.between(Instant.now(), notAfter).toDays();
            if (validForDays < sslCertInfoProperties.getErrorInDays()) {
                LOG.error("Your ssl client auth cert, used to auth at solace broker is going to be expired in {}days", validForDays);
            } else if (validForDays < sslCertInfoProperties.getWarnInDays()) {
                LOG.warn("Your ssl client auth cert, used to auth at solace broker is going to be expired in {}days", validForDays);
            }
        }, workDayBegin, Duration.ofDays(1));
    }

    @SuppressWarnings("unchecked")
    private void unsetProperty(JCSMPProperties jcsmpProperties, String key) {
        try {
            Field propertiesField = JCSMPPropertyMap.class.getDeclaredField("_properties");
            propertiesField.setAccessible(true);
            HashMap<String, Object> properties = (HashMap<String, Object>) propertiesField.get(jcsmpProperties);
            properties.remove(key);
        } catch (Exception ex) {
            LOG.error("unsetProperty failed", ex);
        }
    }

    private void unsetTrustStore(JCSMPProperties jcsmpProperties) {
        try {
            Field isTrustStoreSetField = JCSMPPropertyMap.class.getDeclaredField("_isTrustStoreSet");
            isTrustStoreSetField.setAccessible(true);
            isTrustStoreSetField.set(jcsmpProperties, false);
        } catch (Exception ex) {
            LOG.error("unsetTrustStore failed", ex);
        }
    }


}
