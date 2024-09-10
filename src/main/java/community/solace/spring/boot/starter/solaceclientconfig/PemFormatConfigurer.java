package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * This class configures Solace JCSMP to enable certificates and private keys in the PEM format. This configuration class needs to be
 * added explicitly to every Solace binder context, that is using PEM encoded credentials and certificates. This can be achieved,
 * by setting the {@code environment.spring.main.sources} property in the Solace binder configuration to the FQN of this class.
 */
@AutoConfiguration(before = {
        com.solace.spring.boot.autoconfigure.SolaceJavaAutoConfiguration.class
})
@EnableConfigurationProperties(SslCertInfoProperties.class)
public class PemFormatConfigurer {

    /**
     * Enables Solace configuration to take additional authentication properties and creates a bean post processor.
     *
     * @return bean post processor for JCSMP configuration properties
     */
    @Bean
    public static BeanPostProcessor jcsmpPropertiesPostProcessor() {
        JCSMPPropertiesExtension.enableExtendedAuthenticationProperties();
        return new JCSMPPropertiesPostProcessor(new KeyStoreFactory(new PemFormatTransformer()));
    }
}
