package community.solace.spring.boot.starter.solaceclientconfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyStoreFactoryTest {

    private static final String PEM_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIFaDCCBFCgAwIBAgISESHkvZFwK9Qz0KsXD3x8p44aMA0GCSqGSIb3DQEBCwUA
            ...
            lffygD5IymCSuuDim4qB/9bh7oi37heJ4ObpBIzroPUOthbG4gv/5blW3Dc=
            -----END CERTIFICATE-----""";
    private static final String PEM_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDBj08sp5++4anG
            ...
            z3P668YfhUbKdRF6S42Cg6zn
            -----END PRIVATE KEY-----""";

    private KeyStoreFactory uut;

    @Mock
    private PemFormatTransformer pemFormatTransformerMock;

    @Mock
    private X509Certificate certificateMock;

    @Mock
    private Certificate unsuitableCertificateTypeMock;

    @Mock
    private PrivateKey privateKeyMock;

    @BeforeEach
    void beforeEachTest() {
        uut = new KeyStoreFactory(pemFormatTransformerMock);
    }

    @AfterEach
    void afterEachTest() {
        verifyNoMoreInteractions(pemFormatTransformerMock);
    }

    @Test
    void createClientKeyStore_mustCreateKeyStoreObjectWithCertificates_whenCertificatesTypeCorrect() throws GeneralSecurityException,
            IOException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenReturn(privateKeyMock);
        when(privateKeyMock.getFormat()).thenReturn("PKCS#8");
        when(privateKeyMock.getEncoded()).thenReturn("encoded key".getBytes());
        when(pemFormatTransformerMock.getCertificates(anyString())).thenReturn(new Certificate[]{ certificateMock });

        final KeyStore clientKeyStore = uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE);

        assertThat(clientKeyStore.isKeyEntry("pk"), is(true));
    }

    @Test
    void createClientKeyStore_mustThrowException_whenCertificatesTypeIncorrect() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenReturn(privateKeyMock);
        when(pemFormatTransformerMock.getCertificates(any())).thenReturn(new Certificate[]{ unsuitableCertificateTypeMock });

        assertThrows(IllegalArgumentException.class, () -> uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE));
    }

    @Test
    void createClientKeyStore_mustRethrowCertificateException_always() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenReturn(privateKeyMock);
        when(pemFormatTransformerMock.getCertificates(any())).thenThrow(new CertificateException());

        assertThrows(IllegalArgumentException.class, () -> uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE));
    }

    @Test
    void createClientKeyStore_mustRethrowIOException_always() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenReturn(privateKeyMock);
        when(pemFormatTransformerMock.getCertificates(any())).thenThrow(new IOException());

        assertThrows(IllegalArgumentException.class, () -> uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE));
    }

    @Test
    void createClientKeyStore_mustRethrowNoSuchAlgorithmException_always() throws GeneralSecurityException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenThrow(NoSuchAlgorithmException.class);

        assertThrows(NoSuchAlgorithmException.class, () -> uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE));
    }

    @Test
    void createClientKeyStore_mustRethrowInvalidKeySpecException_always() throws GeneralSecurityException {
        when(pemFormatTransformerMock.getPrivateKey(anyString())).thenThrow(InvalidKeySpecException.class);

        assertThrows(InvalidKeySpecException.class, () -> uut.createClientKeyStore(PEM_PRIVATE_KEY, PEM_CERTIFICATE));
    }

    @Test
    void createTrustStore_mustCreateKeyStoreObjectWithCertificates_whenCertificatesTypeCorrect() throws GeneralSecurityException,
            IOException {
        when(pemFormatTransformerMock.getCertificates(any())).thenReturn(new Certificate[]{ certificateMock, certificateMock });

        final KeyStore trustStore = uut.createTrustStore(PEM_CERTIFICATE);

        assertThat(trustStore.getCertificate("ts0"), is(sameInstance(certificateMock)));
        assertThat(trustStore.getCertificate("ts1"), is(sameInstance(certificateMock)));
    }

    @Test
    void createTrustStore_mustThrowException_whenCertificatesTypeIncorrect() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getCertificates(any())).thenReturn(new Certificate[]{ unsuitableCertificateTypeMock });

        assertThrows(IllegalArgumentException.class, () -> uut.createTrustStore(PEM_CERTIFICATE));
    }

    @Test
    void createTrustStore_mustRethrowCertificateException_always() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getCertificates(any())).thenThrow(new CertificateException());

        assertThrows(IllegalArgumentException.class, () -> uut.createTrustStore(PEM_CERTIFICATE));
    }

    @Test
    void createTrustStore_mustRethrowIOException_always() throws GeneralSecurityException, IOException {
        when(pemFormatTransformerMock.getCertificates(any())).thenThrow(new IOException());

        assertThrows(IllegalArgumentException.class, () -> uut.createTrustStore(PEM_CERTIFICATE));
    }
}
