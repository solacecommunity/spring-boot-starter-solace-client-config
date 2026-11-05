package community.solace.spring.boot.starter.solaceclientconfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Proves that an EC client certificate and its PKCS#8 key, as a CA issues them for an ECDSA certificate request,
 * end up in the client key store. The JDK has no public API to issue a certificate, so the keytool of the running
 * JDK issues a fresh one per run and no key material has to be committed.
 */
class KeyStoreFactoryEcKeyTest {

    private static final char[] GENERATED_STORE_PASSWORD = "generatedStorePassword".toCharArray();
    private static final String CLIENT_ALIAS = "client";

    @TempDir
    static Path generatedStoreDirectory;

    private static String clientKeyPem;
    private static String clientCertificatePem;

    private final KeyStoreFactory uut = new KeyStoreFactory(new PemFormatTransformer());

    @BeforeAll
    static void issueEcClientCertificate() throws IOException, InterruptedException, GeneralSecurityException {
        final Path generatedStore = generatedStoreDirectory.resolve("ec-client.p12");
        final Process keytool = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", CLIENT_ALIAS, "-keyalg", "EC", "-groupname", "secp256r1",
                "-dname", "CN=ec-client", "-validity", "1",
                "-storetype", "PKCS12", "-keystore", generatedStore.toString(),
                "-storepass", new String(GENERATED_STORE_PASSWORD))
                .redirectErrorStream(true)
                .start();
        final String keytoolOutput = new String(keytool.getInputStream().readAllBytes(), UTF_8);
        assertThat("keytool could not issue the EC test certificate: " + keytoolOutput, keytool.waitFor(), equalTo(0));

        final KeyStore issued = KeyStore.getInstance("PKCS12");
        try (InputStream issuedStore = Files.newInputStream(generatedStore)) {
            issued.load(issuedStore, GENERATED_STORE_PASSWORD);
        }
        clientKeyPem = toPem("PRIVATE KEY", issued.getKey(CLIENT_ALIAS, GENERATED_STORE_PASSWORD).getEncoded());
        clientCertificatePem = toPem("CERTIFICATE", issued.getCertificate(CLIENT_ALIAS).getEncoded());
    }

    @Test
    void createClientKeyStore_mustStoreEcKey_whenCertificateMatchesKey() throws GeneralSecurityException {
        final KeyStore clientKeyStore = uut.createClientKeyStore(clientKeyPem, clientCertificatePem);

        assertThat("No client key store was created, the EC key or certificate was rejected - see the WARN log above",
                clientKeyStore, notNullValue());
        final Key storedKey = clientKeyStore.getKey("pk", KeyStoreFactory.INTERNAL_PASSWORD.toCharArray());
        assertThat(storedKey.getAlgorithm(), equalTo("EC"));
    }

    @Test
    void createClientKeyStore_mustReturnNull_whenEcCertificateBelongsToAnotherKey() throws GeneralSecurityException {
        final String otherEcKeyPem = toPem("PRIVATE KEY", KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate().getEncoded());

        assertNull(uut.createClientKeyStore(otherEcKeyPem, clientCertificatePem));
    }

    private static String toPem(final String type, final byte[] der) {
        return "-----BEGIN " + type + "-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes(US_ASCII)).encodeToString(der) +
                "\n-----END " + type + "-----\n";
    }
}
