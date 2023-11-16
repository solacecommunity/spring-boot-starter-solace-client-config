package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
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
final class JCSMPPropertiesPostProcessor implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JCSMPPropertiesPostProcessor.class);

    private final KeyStoreFactory keyStoreFactory;
    private final TaskScheduler taskScheduler;
    private final SslCertInfoProperties sslCertInfoProperties;

    public JCSMPPropertiesPostProcessor(final KeyStoreFactory keyStoreFactory, TaskScheduler taskScheduler, SslCertInfoProperties sslCertInfoProperties) {
        this.keyStoreFactory = keyStoreFactory;
        this.taskScheduler = taskScheduler;
        this.sslCertInfoProperties = sslCertInfoProperties;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        try {
            if (bean instanceof JCSMPProperties) {
                LOG.debug("Postprocessing {} bean", bean.getClass());
                return addAuthenticationProperties(((JCSMPProperties) bean));
            }
        }
        catch (final GeneralSecurityException | IOException | NoSuchFieldException | IllegalAccessException e) {
            LOG.error("Could not postprocess bean {}", bean.getClass());
            throw new FatalBeanException("Failed to enhance JCSMPProperties on bean " + beanName, e);
        }
        return bean;
    }

    private JCSMPProperties addAuthenticationProperties(final JCSMPProperties jcsmpProperties) throws GeneralSecurityException,
            IOException, NoSuchFieldException, IllegalAccessException {

        if (clientCertPropertiesArePresent(jcsmpProperties)) {
            LOG.debug("Adding Solace ClientCert properties to JSCMPProperties");
            KeyStore keyStore = keyStoreFactory.createClientKeyStore(
                    jcsmpProperties.getStringProperty(SSL_PRIVATE_KEY),
                    jcsmpProperties.getStringProperty(SSL_CLIENT_CERT)
            );
            checkValidToPeriodically(keyStoreFactory.getValidTo(jcsmpProperties.getStringProperty(SSL_CLIENT_CERT)));

            jcsmpProperties.setProperty(SSL_IN_MEMORY_KEY_STORE, keyStore);
            jcsmpProperties.setProperty(SSL_KEY_STORE_PASSWORD, keyStoreFactory.getClientKeyStorePassword());
        }
        else if (LOG.isDebugEnabled()) {
            LOG.debug("No Solace ClientCert properties were added to JCSMPProperties. " +
                    "Missing at least one required property: {}, {}, {}", SSL_CLIENT_CERT, SSL_PRIVATE_KEY, SSL_TRUST_CERT);
        }


        if (trustStorePropertiesArePresent(jcsmpProperties)) {
            LOG.debug("Adding Solace TrustStore properties to JSCMPProperties");
            jcsmpProperties.setProperty(SSL_IN_MEMORY_TRUST_STORE, keyStoreFactory.createTrustStore(
                    jcsmpProperties.getStringProperty(SSL_TRUST_CERT))
            );
        }
        else if (LOG.isDebugEnabled()) {
            LOG.debug("No Solace TrustStore properties were added to JCSMPProperties. " +
                    "Missing at least one required property: {}, {}, {}", SSL_CLIENT_CERT, SSL_PRIVATE_KEY, SSL_TRUST_CERT);
        }

        // Solace config is struggling with empty strings.
        if (jcsmpProperties.getProperty("SSL_KEY_STORE") instanceof String &&
                !StringUtils.hasText((CharSequence) jcsmpProperties.getProperty("SSL_KEY_STORE"))) {
            jcsmpProperties.setProperty("SSL_KEY_STORE", "");
            jcsmpProperties.setProperty(SSL_KEY_STORE_PASSWORD, KeyStoreFactory.INTERNAL_PASSWORD);
            unsetProperty(jcsmpProperties, "SSL_PRIVATE_KEY_PASSWORD");
            unsetProperty(jcsmpProperties, "SSL_PRIVATE_KEY_ALIAS");
        }

        if (jcsmpProperties.getProperty("SSL_TRUST_STORE") instanceof String &&
                !StringUtils.hasText((CharSequence) jcsmpProperties.getProperty("SSL_TRUST_STORE"))) {
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

        // Always log at 09:00AM to not trigger nightly support on error.
        Instant workDayBegin = Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .toInstant();
        if (Instant.now().isAfter(workDayBegin)) {
            workDayBegin = workDayBegin.plus(1, ChronoUnit.DAYS);
        }

        taskScheduler.scheduleAtFixedRate(
                () -> {
                    long validForDays = Duration.between(Instant.now(), notAfter).toDays();
                    if (validForDays < sslCertInfoProperties.getErrorInDays()) {
                        LOG.error("Your ssl client auth cert, used to auth at solace broker is going to be expired in " + validForDays + "days");
                    } else if (validForDays < sslCertInfoProperties.getWarnInDays()) {
                        LOG.warn("Your ssl client auth cert, used to auth at solace broker is going to be expired in " + validForDays + "days");
                    }
                },
                workDayBegin,
                Duration.ofDays(1)
        );
    }

    @SuppressWarnings("unchecked")
    private void unsetProperty(JCSMPProperties jcsmpProperties, String key) throws NoSuchFieldException, IllegalAccessException {
        Field propertiesField = JCSMPPropertyMap.class.getDeclaredField("_properties");
        propertiesField.setAccessible(true);

        HashMap<String, Object> properties = (HashMap<String, Object>) propertiesField.get(jcsmpProperties);
        properties.remove(key);
    }

    private void unsetTrustStore(JCSMPProperties jcsmpProperties) throws NoSuchFieldException, IllegalAccessException {
        Field isTrustStoreSetField = JCSMPPropertyMap.class.getDeclaredField("_isTrustStoreSet");
        isTrustStoreSetField.setAccessible(true);

        isTrustStoreSetField.set(jcsmpProperties, false);
    }

    private boolean clientCertPropertiesArePresent(final JCSMPProperties jcsmpProperties) {
        return AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE.equals(jcsmpProperties.getStringProperty(AUTHENTICATION_SCHEME)) &&
                StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_PRIVATE_KEY)) &&
                StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_CLIENT_CERT));
    }

    private boolean trustStorePropertiesArePresent(final JCSMPProperties jcsmpProperties) {
        return AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE.equals(jcsmpProperties.getStringProperty(AUTHENTICATION_SCHEME)) &&
                StringUtils.hasText(jcsmpProperties.getStringProperty(SSL_TRUST_CERT));
    }
}
