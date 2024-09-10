package community.solace.spring.boot.starter.solaceclientconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PemFormatTransformerTest {

    private PemFormatTransformer uut;

    @BeforeEach
    void beforeEachTest() {
        uut = new PemFormatTransformer();
    }

    @Test
    void getPrivateKeyGeneratesCorrectKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        final PrivateKey privateKey = uut.getPrivateKey(getPemFromProperties("SOLACE_PRIVATE_KEY"));
        assertThat(privateKey, notNullValue());
    }

    @Test
    void getPrivateKey_throwsIllegalArgumentException_ifPemStringIsInvalid() {
        final String solacePrivateKey = "-----XXX PRIVATE KEY-----PrivateKey-----YYY PRIVATE KEY-----";
        assertNull(uut.getPrivateKey(solacePrivateKey));
    }

    @Test
    void getCertificatesGeneratesCorrectTrustCertificates() throws CertificateException, IOException {
        final Certificate[] certificate = uut.getCertificates(getPemFromProperties("SOLACE_TRUST_ROOTS"), "SOLACE_TRUST_ROOTS");
        assertThat(certificate, notNullValue());
        assertThat(certificate.length, equalTo(2));
    }

    @Test
    void getCertificatesGeneratesCorrectClientCertificates() throws CertificateException, IOException {
        final Certificate[] certificate = uut.getCertificates(getPemFromProperties("SOLACE_CLIENT_CERT"), "SOLACE_CLIENT_CERT");
        assertThat(certificate, notNullValue());
        assertThat(certificate.length, equalTo(1));
    }

    private String getPemFromProperties(final String key) throws IOException {
        final Resource resource = new ClassPathResource("solace.properties");
        final Properties solaceProperties = PropertiesLoaderUtils.loadProperties(resource);
        return solaceProperties.getProperty(key);
    }
}
