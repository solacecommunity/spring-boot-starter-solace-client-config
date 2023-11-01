/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2023.
 */

package ch.sbb.tms.platform.springbootstarter.solaceclientconfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

final class KeyStoreFactory {

    static final String INTERNAL_PASSWORD = "internalPassword";

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
}
