package community.solace.spring.boot.starter.solaceclientconfig;

import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.SolaceSessionOAuth2TokenProvider;
import com.solacesystems.jcsmp.SpringJCSMPFactory;
import com.solacesystems.jcsmp.impl.JCSMPPropertiesExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@Configuration
@AutoConfigureBefore(JmsAutoConfiguration.class)
@ConditionalOnClass({JCSMPProperties.class})
@ConditionalOnMissingBean(SpringJCSMPFactory.class)
@EnableConfigurationProperties({
        SolaceJavaProperties.class,
        SslCertInfoProperties.class
})
@Import(SolaceOAuthClientConfiguration.class)
public class SolaceJavaAutoConfiguration {

    private SolaceJavaProperties properties;

    @Autowired
    public SolaceJavaAutoConfiguration(SolaceJavaProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns a {@link SpringJCSMPFactory} based on {@link JCSMPProperties} bean.
     *
     * @return {@link SpringJCSMPFactory} based on {@link JCSMPProperties} bean.
     */
    @Bean
    public SpringJCSMPFactory getSpringJCSMPFactory(JCSMPProperties jcsmpProperties,
                                                    @Nullable SolaceSessionOAuth2TokenProvider solaceSessionOAuth2TokenProvider) {
        return new SpringJCSMPFactory(jcsmpProperties, solaceSessionOAuth2TokenProvider);
    }

    /**
     * Returns a {@link JCSMPProperties} based on {@link SolaceJavaProperties}.
     *
     * @return {@link JCSMPProperties} based on {@link SolaceJavaProperties}.
     */
    private static final Logger logger = LoggerFactory.getLogger(SolaceJavaAutoConfiguration.class);

    @Bean
    public JCSMPProperties getJCSMPProperties(
            Optional<TaskScheduler> taskScheduler,
            SslCertInfoProperties sslCertInfoProperties
    ) {
        try {
            Class.forName("io.netty.channel.MultiThreadIoEventLoopGroup");
        } catch (Throwable e) {
            logger.error("To old jetty version detected. Add '<properties><netty.version>4.2.7.Final</netty.version></properties>' to your pom.xml");
            System.exit(1);
        }


        JCSMPPropertiesExtension.enableExtendedAuthenticationProperties();

        Properties p = new Properties();
        Set<Entry<String, String>> set = properties.getApiProperties().entrySet();
        for (Map.Entry<String, String> entry : set) {
            p.put("jcsmp." + entry.getKey(), entry.getValue());
        }

        JCSMPProperties jcsmpProps = createFromApiProperties(p);
        jcsmpProps.setProperty(JCSMPProperties.HOST, properties.getHost());
        jcsmpProps.setProperty(JCSMPProperties.VPN_NAME, properties.getMsgVpn());
        jcsmpProps.setProperty(JCSMPProperties.USERNAME, properties.getClientUsername());
        jcsmpProps.setProperty(JCSMPProperties.PASSWORD, properties.getClientPassword());
        if ((properties.getClientName() != null) && (!properties.getClientName().isEmpty())) {
            jcsmpProps.setProperty(JCSMPProperties.CLIENT_NAME, properties.getClientName());
        }

        // Channel Properties
        JCSMPChannelProperties cp = (JCSMPChannelProperties) jcsmpProps
                .getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
        cp.setConnectRetries(properties.getConnectRetries());
        cp.setReconnectRetries(properties.getReconnectRetries());
        cp.setConnectRetriesPerHost(properties.getConnectRetriesPerHost());
        cp.setReconnectRetryWaitInMillis(properties.getReconnectRetryWaitInMillis());

        if (properties.getOauth2ClientRegistrationId() != null) {
            jcsmpProps.setProperty(SolaceJavaProperties.SPRING_OAUTH2_CLIENT_REGISTRATION_ID,
                    properties.getOauth2ClientRegistrationId());
        }




        JCSMPAuthenticationPropertiesPostProcessor postProcessor = new JCSMPAuthenticationPropertiesPostProcessor(
                new KeyStoreFactory(new PemFormatTransformer()),
                taskScheduler.orElse(null),
                sslCertInfoProperties
        );
        return postProcessor.addAuthenticationProperties(jcsmpProps, properties);
    }


    private JCSMPProperties createFromApiProperties(Properties apiProps) {
        return apiProps != null ? JCSMPProperties.fromProperties(apiProps) : new JCSMPProperties();
    }


    void setProperties(SolaceJavaProperties properties) {
        this.properties = properties;
    }
}
