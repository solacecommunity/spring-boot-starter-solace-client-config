/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2023.
 */
package ch.sbb.tms.platform.springbootstarter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;

import static com.solacesystems.jcsmp.JCSMPProperties.AUTHENTICATION_SCHEME;
import static com.solacesystems.jcsmp.JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_IN_MEMORY_KEY_STORE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_IN_MEMORY_TRUST_STORE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_KEY_STORE_PASSWORD;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_CLIENT_CERT;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_PRIVATE_KEY;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_TRUST_CERT;

/**
 * A bean post processor used to enhance the JCSMP configuration properties with authentication objects created from the PEM format.
 */
final class JCSMPPropertiesPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCSMPPropertiesPostProcessor.class);

    private final KeyStoreFactory keyStoreFactory;

    public JCSMPPropertiesPostProcessor(final KeyStoreFactory keyStoreFactory) {
        this.keyStoreFactory = keyStoreFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        try {
            if (bean instanceof JCSMPProperties) {
                LOGGER.debug("Postprocessing {} bean", bean.getClass());
                return addAuthenticationProperties(((JCSMPProperties) bean));
            }
        }
        catch (final GeneralSecurityException | IOException | NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Could not postprocess bean {}", bean.getClass());
            throw new FatalBeanException("Failed to enhance JCSMPProperties on bean " + beanName, e);
        }
        return bean;
    }

    private JCSMPProperties addAuthenticationProperties(final JCSMPProperties jcsmpProperties) throws GeneralSecurityException,
            IOException, NoSuchFieldException, IllegalAccessException {

        if (clientCertPropertiesArePresent(jcsmpProperties)) {
            LOGGER.debug("Adding Solace ClientCert properties to JSCMPProperties");
            jcsmpProperties.setProperty(SSL_IN_MEMORY_KEY_STORE, keyStoreFactory.createClientKeyStore(
                    jcsmpProperties.getStringProperty(SSL_PRIVATE_KEY),
                    jcsmpProperties.getStringProperty(SSL_CLIENT_CERT)
            ));
            jcsmpProperties.setProperty(SSL_KEY_STORE_PASSWORD, keyStoreFactory.getClientKeyStorePassword());
        }
        else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("No Solace ClientCert properties were added to JCSMPProperties. " +
                    "Missing at least one required property: {}, {}, {}", SSL_CLIENT_CERT, SSL_PRIVATE_KEY, SSL_TRUST_CERT);
        }


        if (trustStorePropertiesArePresent(jcsmpProperties)) {
            LOGGER.debug("Adding Solace TrustStore properties to JSCMPProperties");
            jcsmpProperties.setProperty(SSL_IN_MEMORY_TRUST_STORE, keyStoreFactory.createTrustStore(
                    jcsmpProperties.getStringProperty(SSL_TRUST_CERT))
            );
        }
        else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("No Solace TrustStore properties were added to JCSMPProperties. " +
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
