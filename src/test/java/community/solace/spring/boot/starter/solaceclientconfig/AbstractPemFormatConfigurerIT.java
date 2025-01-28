package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.*;
import java.security.cert.Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
abstract class AbstractPemFormatConfigurerIT {

    static final String JCSMP_PROPERTIES_NAME = "getJCSMPProperties";

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private SslCertInfoProperties sslCertInfoProperties;

    @Test
    void jcsmpPropertiesAreFullySet() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        final JCSMPProperties jcsmpProperties = context.getBean(JCSMP_PROPERTIES_NAME, JCSMPProperties.class);

        final String password = (String) jcsmpProperties.getProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD);
        assertThat(password, equalTo("internalPassword"));

        final KeyStore keyStore = (KeyStore) jcsmpProperties.getProperty(JCSMPProperties.SSL_IN_MEMORY_KEY_STORE);
        final Key key = keyStore.getKey("pk", "internalPassword".toCharArray());
        assertThat(key, notNullValue());

        final KeyStore trustStore = (KeyStore) jcsmpProperties.getProperty(JCSMPProperties.SSL_IN_MEMORY_TRUST_STORE);
        final Certificate certificate = trustStore.getCertificate("ts1");
        assertThat(certificate, notNullValue());
    }
}
