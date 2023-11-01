/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2023.
 */

package com.solacesystems.jcsmp.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;

class JCSMPPropertiesExtensionTest {

    @Test
    void enableExtendedAuthenticationProperties_mustAddPropertySetters_always() {
        JCSMPPropertiesExtension.enableExtendedAuthenticationProperties();

        final Map<String, JCSMPPropertiesTextMarshaling.PropertySetter> propertySetters = JCSMPPropertiesTextMarshaling.pv_psetters;
        assertThat(propertySetters, hasKey("SSL_CLIENT_CERT"));
        assertThat(propertySetters, hasKey("SSL_PRIVATE_KEY"));
        assertThat(propertySetters, hasKey("SSL_TRUST_CERT"));
    }
}
