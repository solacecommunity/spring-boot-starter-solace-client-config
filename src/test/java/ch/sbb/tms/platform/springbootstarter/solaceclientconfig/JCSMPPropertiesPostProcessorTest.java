/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2023.
 */

package ch.sbb.tms.platform.springbootstarter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.FatalBeanException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static com.solacesystems.jcsmp.JCSMPProperties.AUTHENTICATION_SCHEME;
import static com.solacesystems.jcsmp.JCSMPProperties.AUTHENTICATION_SCHEME_BASIC;
import static com.solacesystems.jcsmp.JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_IN_MEMORY_KEY_STORE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_IN_MEMORY_TRUST_STORE;
import static com.solacesystems.jcsmp.JCSMPProperties.SSL_KEY_STORE_PASSWORD;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_CLIENT_CERT;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_PRIVATE_KEY;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.SSL_TRUST_CERT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JCSMPPropertiesPostProcessorTest {

    private static final String TEST_PASSWORD = "internalPassword";

    private JCSMPPropertiesPostProcessor uut;

    @Mock
    private KeyStoreFactory keyStoreFactoryMock;

    @Mock
    private KeyStore keyStoreMock;

    @Mock
    private KeyStore trustStoreMock;

    @BeforeEach
    void beforeEachTest() {
        uut = new JCSMPPropertiesPostProcessor(keyStoreFactoryMock);
    }

    @Test
    void postProcessBeforeInitialization_mustSetAuthenticationProperties_whenAllRequiredPropertiesPresent() throws GeneralSecurityException, IOException {
        when(keyStoreFactoryMock.createClientKeyStore(any(), any())).thenReturn(keyStoreMock);
        when(keyStoreFactoryMock.getClientKeyStorePassword()).thenReturn(TEST_PASSWORD);
        when(keyStoreFactoryMock.createTrustStore(any())).thenReturn(trustStoreMock);
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = (JCSMPProperties) uut.postProcessBeforeInitialization(jcsmpProperties, "props");

        assertThat(extendedJcsmpProperties, is(notNullValue()));
        assertThat(extendedJcsmpProperties.getProperty(AUTHENTICATION_SCHEME), is(sameInstance(AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE)));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_KEY_STORE), is(sameInstance(keyStoreMock)));
        assertThat(extendedJcsmpProperties.getProperty(SSL_KEY_STORE_PASSWORD), is(equalTo(TEST_PASSWORD)));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_TRUST_STORE), is(sameInstance(trustStoreMock)));
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenClientCertificateIsMissing() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = (JCSMPProperties) uut.postProcessBeforeInitialization(jcsmpProperties, "props");

        assertThat(extendedJcsmpProperties, is(notNullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_KEY_STORE), is(nullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_KEY_STORE_PASSWORD), is("internalPassword"));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_TRUST_STORE), is(nullValue()));
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenAuthenticationSchemeNotCompatible() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_BASIC);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = (JCSMPProperties) uut.postProcessBeforeInitialization(jcsmpProperties, "props");

        assertThat(extendedJcsmpProperties, is(notNullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_KEY_STORE), is(nullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_KEY_STORE_PASSWORD), is("internalPassword"));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_TRUST_STORE), is(nullValue()));
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenPrivateKeyIsMissing() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = (JCSMPProperties) uut.postProcessBeforeInitialization(jcsmpProperties, "props");

        assertThat(extendedJcsmpProperties, is(notNullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_KEY_STORE), is(nullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_KEY_STORE_PASSWORD), is("internalPassword"));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_TRUST_STORE), is(nullValue()));
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenTrustCertificateIsMissing() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");

        final JCSMPProperties extendedJcsmpProperties = (JCSMPProperties) uut.postProcessBeforeInitialization(jcsmpProperties, "props");

        assertThat(extendedJcsmpProperties, is(notNullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_KEY_STORE), is(nullValue()));
        assertThat(extendedJcsmpProperties.getProperty(SSL_KEY_STORE_PASSWORD), is("internalPassword"));
        assertThat(extendedJcsmpProperties.getProperty(SSL_IN_MEMORY_TRUST_STORE), is(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(classes = {
            CertificateException.class,
            IOException.class,
            NoSuchAlgorithmException.class,
            InvalidKeySpecException.class
    })
    void postProcessBeforeInitialization_mustThrowFatalBeanException_whenKeyStoreFactoryThrowsException(final Class<? extends Exception> exceptionType) throws GeneralSecurityException, IOException {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");
        when(keyStoreFactoryMock.createClientKeyStore(any(), any())).thenThrow(exceptionType);

        assertThrows(FatalBeanException.class, () -> uut.postProcessBeforeInitialization(jcsmpProperties, "props"));
    }

    @ParameterizedTest
    @ValueSource(classes = {
            CertificateException.class,
            IOException.class
    })
    void postProcessBeforeInitialization_mustThrowFatalBeanException_whenKeyStoreFactoryThrowsExceptionForTrustCerts(
            final Class<? extends Exception> exceptionType) throws GeneralSecurityException, IOException {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");
        when(keyStoreFactoryMock.createTrustStore(any())).thenThrow(exceptionType);

        assertThrows(FatalBeanException.class, () -> uut.postProcessBeforeInitialization(jcsmpProperties, "props"));
    }

}
