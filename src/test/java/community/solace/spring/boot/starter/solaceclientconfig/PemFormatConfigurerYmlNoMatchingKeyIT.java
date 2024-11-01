package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.KeyStore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

@ActiveProfiles("pem-from-yml-not-matching-key")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
@ExtendWith(OutputCaptureExtension.class)
class PemFormatConfigurerYmlNoMatchingKeyIT {
    static final String JCSMP_PROPERTIES_NAME = "getJCSMPProperties";

    @Autowired
    private ApplicationContext context;

    @MockBean
    private SslCertInfoProperties sslCertInfoProperties;

    @Test
    void jcsmpPropertiesAreFullySet(CapturedOutput output) {
        final JCSMPProperties jcsmpProperties = context.getBean(JCSMP_PROPERTIES_NAME, JCSMPProperties.class);

        final String password = (String) jcsmpProperties.getProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD);
        assertThat(password, nullValue());

        final KeyStore keyStore = (KeyStore) jcsmpProperties.getProperty(JCSMPProperties.SSL_IN_MEMORY_KEY_STORE);
        assertThat(keyStore, nullValue());

        final KeyStore trustStore = (KeyStore) jcsmpProperties.getProperty(JCSMPProperties.SSL_IN_MEMORY_TRUST_STORE);
        assertThat(trustStore, nullValue());


        assertThat(output.getAll(), containsString("Non of the given certificates in SSL_CLIENT_CERT matches the give key SSL_PRIVATE_KEY"));
    }
}
