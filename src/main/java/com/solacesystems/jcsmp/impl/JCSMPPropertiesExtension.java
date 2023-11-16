package com.solacesystems.jcsmp.impl;

import java.util.Map;

import static com.solacesystems.jcsmp.impl.JCSMPPropertiesTextMarshaling.STRINGPARSER;

/**
 * This class configures the {@link com.solacesystems.jcsmp.JCSMPProperties} class to take additional properties used to pass the
 * certificates and private key in the PEM format.
 */
public final class JCSMPPropertiesExtension {

    /**
     * Solace API property to set the client certificate in the PEM format.
     */
    public static final String SSL_CLIENT_CERT = "SSL_CLIENT_CERT";

    /**
     * Solace API property to set the client private key in the PEM format.
     */
    public static final String SSL_PRIVATE_KEY = "SSL_PRIVATE_KEY";

    /**
     * Solace API property to set the trust certificate in the PEM format.
     */
    public static final String SSL_TRUST_CERT = "SSL_TRUST_CERT";

    /**
     * Enables the Solace configuration to take additional authentication properties in the PEM format.
     */
    public static void enableExtendedAuthenticationProperties() {
        final Map<String, JCSMPPropertiesTextMarshaling.PropertySetter> propertySetters = JCSMPPropertiesTextMarshaling.pv_psetters;
        propertySetters.put(SSL_CLIENT_CERT, new JCSMPPropertiesTextMarshaling.BasicPropertySetter(SSL_CLIENT_CERT, SSL_CLIENT_CERT, STRINGPARSER));
        propertySetters.put(SSL_PRIVATE_KEY, new JCSMPPropertiesTextMarshaling.BasicPropertySetter(SSL_PRIVATE_KEY, SSL_PRIVATE_KEY, STRINGPARSER));
        propertySetters.put(SSL_TRUST_CERT, new JCSMPPropertiesTextMarshaling.BasicPropertySetter(SSL_TRUST_CERT, SSL_TRUST_CERT, STRINGPARSER));
    }

    private JCSMPPropertiesExtension() {
        // no instantiation
    }
}
