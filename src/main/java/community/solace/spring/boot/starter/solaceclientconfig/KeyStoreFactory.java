package community.solace.spring.boot.starter.solaceclientconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;

final class KeyStoreFactory {

    static final String INTERNAL_PASSWORD = "internalPassword";
    private static final Logger LOG = LoggerFactory.getLogger(KeyStoreFactory.class);

    private final PemFormatTransformer pemFormatTransformer;

    KeyStoreFactory(final PemFormatTransformer pemFormatTransformer) {
        this.pemFormatTransformer = pemFormatTransformer;
    }

    KeyStore createClientKeyStore(final String privateKeyPem, final String clientCertPem) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        final PrivateKey privateKey = pemFormatTransformer.getPrivateKey(privateKeyPem);
        try {
            final Certificate[] certificates = pemFormatTransformer.getCertificates(clientCertPem);
            keyStore.setKeyEntry("pk", privateKey, INTERNAL_PASSWORD.toCharArray(), certificates);
            return keyStore;
        } catch (Exception e) {
            throw new IllegalArgumentException("SSL_CLIENT_CERT: " + e.getMessage(), e);
        }
    }

    String getClientKeyStorePassword() {
        return INTERNAL_PASSWORD;
    }

    KeyStore createTrustStore(final String trustCertificatesPem) throws GeneralSecurityException, IOException {
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        try {
            final Certificate[] certificates = pemFormatTransformer.getCertificates(trustCertificatesPem);
            for (int i = 0; i < certificates.length; i++) {
                trustStore.setCertificateEntry("ts" + i, certificates[i]);
            }
            return trustStore;
        } catch (Exception e) {
            throw new IllegalArgumentException("SSL_TRUST_CERT: " + e.getMessage(), e);
        }
    }

    public Instant getValidTo(String clientCertPem) {
        try {
            final Certificate[] certificates = pemFormatTransformer.getCertificates(clientCertPem);
            return ((X509Certificate)certificates[0]).getNotAfter().toInstant();
        } catch (CertificateException | IOException e) {
            LOG.error("Unable to extract notAfter date from certificate", e);
            return null;
        }
    }
}
