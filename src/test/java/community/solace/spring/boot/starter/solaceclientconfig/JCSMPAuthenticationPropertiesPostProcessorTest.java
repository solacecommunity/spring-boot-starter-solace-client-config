package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;

import static com.solacesystems.jcsmp.JCSMPProperties.*;
import static com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JCSMPAuthenticationPropertiesPostProcessorTest {

    private static final String TEST_PASSWORD = "internalPassword";

    private JCSMPAuthenticationPropertiesPostProcessor uut;

    @Mock
    private KeyStoreFactory keyStoreFactoryMock;

    @Mock
    private KeyStore keyStoreMock;

    @Mock
    private KeyStore trustStoreMock;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SslCertInfoProperties sslCertInfoProperties;

    @BeforeEach
    void beforeEachTest() {
        uut = new JCSMPAuthenticationPropertiesPostProcessor(
                keyStoreFactoryMock,
                taskScheduler,
                sslCertInfoProperties
        );
    }

    @Test
    void postProcessBeforeInitialization_mustSetAuthenticationProperties_whenAllRequiredPropertiesPresent() throws GeneralSecurityException, IOException {
        when(keyStoreFactoryMock.createClientKeyStore(any(), any())).thenReturn(keyStoreMock);
        when(keyStoreFactoryMock.getClientKeyStorePassword()).thenReturn(TEST_PASSWORD);
        when(keyStoreFactoryMock.createTrustStore(any())).thenReturn(trustStoreMock);
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_CLIENT_CERT, "clientCertificatePemString");
        javaApiProperties.put(SSL_PRIVATE_KEY, "privateKeyPemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);

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

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_PRIVATE_KEY, "privateKeyPemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);

        assertEquals(jcsmpProperties, extendedJcsmpProperties);
        assertSame(jcsmpProperties, extendedJcsmpProperties);
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenAuthenticationSchemeNotCompatible() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_BASIC);


        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);

        final JCSMPProperties extendedJcsmpProperties = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);

        assertEquals(jcsmpProperties, extendedJcsmpProperties);
        assertSame(jcsmpProperties, extendedJcsmpProperties);
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenPrivateKeyIsMissing() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_CLIENT_CERT, "clientCertificatePemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);

        assertEquals(jcsmpProperties, extendedJcsmpProperties);
        assertSame(jcsmpProperties, extendedJcsmpProperties);
    }

    @Test
    void postProcessBeforeInitialization_mustNotSetAuthenticationProperties_whenTrustCertificateIsMissing() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_CLIENT_CERT, "clientCertificatePemString");
        javaApiProperties.put(SSL_PRIVATE_KEY, "privateKeyPemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        final JCSMPProperties extendedJcsmpProperties = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);
        assertEquals(jcsmpProperties, extendedJcsmpProperties);
        assertSame(jcsmpProperties, extendedJcsmpProperties);
    }

    @Test
    void postProcessBeforeInitialization_mustThrowFatalBeanException_whenKeyStoreFactoryThrowsException() throws GeneralSecurityException, IOException {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        when(keyStoreFactoryMock.createClientKeyStore(any(), any())).thenReturn(null);

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_CLIENT_CERT, "clientCertificatePemString");
        javaApiProperties.put(SSL_PRIVATE_KEY, "privateKeyPemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        Object props = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);
        assertEquals(jcsmpProperties, props);
        assertSame(jcsmpProperties, props);
    }

    @Test
    void postProcessBeforeInitialization_mustThrowFatalBeanException_whenKeyStoreFactoryThrowsExceptionForTrustCerts() {
        final JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        jcsmpProperties.setProperty(SSL_CLIENT_CERT, "clientCertificatePemString");
        jcsmpProperties.setProperty(SSL_PRIVATE_KEY, "privateKeyPemString");
        jcsmpProperties.setProperty(SSL_TRUST_CERT, "trustCertificatePemString");

        final SolaceJavaProperties javaProperties = new SolaceJavaProperties();
        Map<String, String> javaApiProperties = javaProperties.getApiProperties();
        javaApiProperties.put(AUTHENTICATION_SCHEME, AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        javaApiProperties.put(SSL_CLIENT_CERT, "clientCertificatePemString");
        javaApiProperties.put(SSL_PRIVATE_KEY, "privateKeyPemString");
        javaApiProperties.put(SSL_TRUST_CERT, "trustCertificatePemString");

        Object props = uut.addAuthenticationProperties(jcsmpProperties, javaProperties);
        assertEquals(jcsmpProperties, props);
        assertSame(jcsmpProperties, props);
    }
}
