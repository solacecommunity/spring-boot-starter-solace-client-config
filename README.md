# spring-boot-starter-solace-client-config

This project provides Spring Boot Auto-Configuration and an associated Spring Boot Starter for the Solace Java API. 
The goal of this project is to make it easy to auto wire the Solace Java API within your application so you can take advantage 
of all the benefits of Spring Boot auto-configuration.

## Spring Cloud Version Compatibility

Consult the table below to determine which version you need to use:

| spring-boot-starter-solace-client-config | Spring Boot | sol-jcsmp |
|------------------------------------------|-------------|-----------|
| 1.1.3                                    | 3.5.8       | 10.29.0   |
| 1.1.2                                    | 3.5.8       | 10.29.0   |
| 1.1.1                                    | 3.5.6       | 10.28.1   |
| 1.1.0                                    | 3.5.4       | 10.27.0   |
| 1.0.5                                    | 3.4.2       | 10.25.2   |
| 1.0.3                                    | 3.3.3       | 10.24.1   |
| 1.0.2                                    | 3.3.0       | 10.23.0   |
| 1.0.1                                    | 3.2.5       | 10.23.0   |

## Usage

Add a dependency in your application POM:
```xml

<dependency>
    <groupId>community.solace.spring.boot</groupId>
    <artifactId>spring-boot-starter-solace-client-config</artifactId>
    <version>1.1.3</version>
</dependency>
```

## Overview

As stated this project provides a Spring Boot Auto-Configuration implementation and a Spring Boot Starter pom for the Solace Java API. 
The goal of this project is to make it easier to use the Solace Java API with Spring Boot auto-configuration through the `@Autowired` annotation.

The artifacts are published to Maven Central so it should be familiar and intuitive to use this project in your applications.

One item to note as described below is that this project introduces a new factory for Solace Java API sessions: `SpringJCSMPFactory`.  
In the future, the Solace Java API may introduce a similar factory and remove the need for this custom extension.  
For now however, this is included in the auto-configuration jar for ease of use.

### Using Spring Dependency Auto-Configuration (@SpringBootApplication & @Autowired)

Now in your application code, you can simply declare the `SpringJCSMPFactory` and annotate it so that it is autowired:

```java
@Autowired
private SpringJCSMPFactory solaceFactory;
```

Once you have the `SpringJCSMPFactory`, it behaves just like the `JCSMPFactory` and can be used to create sessions. For example:

```java
final JCSMPSession session = solaceFactory.createSession();
```

The `SpringJCSMPFactory` is a wrapper of the singleton `JCSMPFactory` which contains an associated `JCSMPProperties`. 
This facilitates auto-wiring by Spring but otherwise maintains the familiar `JCSMPFactory` interface known to users of the Solace Java API.

Alternatively, you could autowire JCSMPProperties to create your own customized `SpringJCSMPFactory`:

```java
/* The properties of a JCSMP connection */
@Autowired
private JCSMPProperties jcsmpProperties;
```

### Configure the Application to use your Solace PubSub+ Service Credentials

The configuration of the `SpringJCSMPFactory` can be done through the `application.properties`. 
This is where users can control the Solace Java API properties. 
Currently this project supports direct configuration of the following properties:

```
solace.java.host
solace.java.msgVpn
solace.java.clientUsername
solace.java.clientPassword
solace.java.clientName
solace.java.connectRetries
solace.java.reconnectRetries
solace.java.connectRetriesPerHost
solace.java.reconnectRetryWaitInMillis
solace.java.oauth2ClientRegistrationId ##Set it when OAuth2 authentication scheme enabled. Reference to the Spring OAuth2 client registration-id.
```

Where reasonable, sensible defaults are always chosen. 
So a developer using a Solace PubSub+ message broker and wishing to use the default message-vpn may only set the `solace.java.host`.

Any additional Solace Java API properties can be set through configuring `solace.java.apiProperties.<Property>` where `<Property>` is the name of the property as defined in the [Solace Java API documentation for `com.solacesystems.jcsmp.JCSMPProperties`](//docs.solace.com/API-Developer-Online-Ref-Documentation/java/constant-values.html#com.solacesystems.jcsmp.JCSMPProperties.ACK_EVENT_MODE), for example:

```
solace.java.apiProperties.reapply_subscriptions=false
solace.java.apiProperties.ssl_trust_store=/path/to/truststore
solace.java.apiProperties.client_channel_properties.keepAliveIntervalInMillis=3000
```

Note that the direct configuration of `solace.java.` properties takes precedence over the `solace.java.apiProperties.`.

## Using SslClient Authentication Scheme

This starter enables the Solace configuration to use certificates and private keys in the PEM format. As PEM is just a text format,
it can be easily passed to the application configuration directly from an environment variable or configuration files such as `.yaml` or
`.properties`. Loading certificates and private keys in the `.jks` format from the file system is no longer necessary.

The PEM strings can be defined via environment variables, properties or directly in the `application.yml`.
```yaml
solace:
  java:
    apiProperties:
      AUTHENTICATION_SCHEME: AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
      SSL_CLIENT_CERT: ${SOLACE_CLIENT_CERT}
      SSL_PRIVATE_KEY: ${SOLACE_PRIVATE_KEY}
      SSL_TRUST_CERT: ${SOLACE_TRUST_ROOTS:}
```

When the configuration is made directly in the binder config, it requires additionally the
`spring.main.sources: community.solace.spring.boot.starter.solaceclientconfig.PemFormatConfigurer` property to enable the
PemFormatConfigurer in the binder context.
```yaml
spring:
  cloud:
    stream:
      binders:
        <solace_binder_name>:
          type: solace
          environment:
            spring.main.sources: community.solace.spring.boot.starter.solaceclientconfig.PemFormatConfigurer
            solace:
              java:
                apiProperties:
                  AUTHENTICATION_SCHEME: AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
                  SSL_CLIENT_CERT: ${SOLACE_CLIENT_CERT}
                  SSL_PRIVATE_KEY: ${SOLACE_PRIVATE_KEY}
                  SSL_TRUST_CERT: ${SOLACE_TRUST_ROOTS:}
```

Note, that for both configuration cases, all 4 API properties (AUTHENTICATION_SCHEME, SSL_CLIENT_CERT, SSL_PRIVATE_KEY, SSL_TRUST_CERT) are required to 
create a valid Solace broker connection configuration.


An additional feature is log messages to warn you if your certificate is going to be expired.

```yaml
solace:
  java:
    sslCertInfo:
      enabled: true
      warnInDays: 30
      errorInDays: 7
```

You will get WARNING or ERROR messages in log like:
`Your ssl client auth cert, used to auth at solace broker is going to be expired in 25days`

that should highlight if your client cert is about to expire.

## Using OAuth2 Authentication Scheme

This Spring Boot starter for Solace Java API supports OAuth2 authentication scheme. It requires a version of Solace PubSub+ broker that supports OAuth2 authentication scheme.

The Solace PubSub+ Broker should be setup for OAuth2 authentication. Refer to
the [Solace PubSub+: Configuring-OAuth-Authorization](https://docs.solace.com/Security/Configuring-OAuth-Authorization.htm)
for more information.
See [Azure OAuth Setup](https://solace.com/blog/azure-oauth-setup-for-solace-rest-and-smf-clients/)
for example.

You may also like to check
the [OAuth2 Integration Test](src/test/java/community/solace/spring/boot/starter/solaceclientconfig/springBootTests/MessagingWithOAuthIT.java)
for more information.

> [!NOTE]
> The OAuth profile on Solace PubSub+ broker should be setup for Resource Server role. This Solace
> Java API Starer OAuth2 authentication scheme supports ```client_credentials``` grant type out-of-the
> box.

> [!TIP]
> The OAuth2 grant type ```client_credentials``` is used for machine to machine authentication, it
> is recommended that Token expiry time is not too short as it may cause frequent token refreshes and
> impact the performance.

### Using OAuth2 Authentication Scheme with Solace Java API

To use OAuth2 authentication scheme with Solace Java API, follow these steps:

Firstly, add the required dependencies to your `build.gradle` file:

```groovy
compile("org.springframework.boot:spring-boot-starter-oauth2-client")
```

or `pom.xml` file:

```xml

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

Secondly, add `@EnableWebSecurity` annotation to your Spring Boot application class:

```java

@SpringBootApplication
@EnableWebSecurity
public class DemoApplication {

}
```

Finally, configure the Spring OAuth2 Client Registration provider through following properties in
your `application.properties` file:

```
##spring.security.oauth2.client.registration.<registration-id>.provider=<provider-id>
spring.security.oauth2.client.registration.my-oauth2-client.provider=my-auth-server
spring.security.oauth2.client.registration.my-oauth2-client.client-id=replace-client-id-here
spring.security.oauth2.client.registration.my-oauth2-client.client-secret=replace-client-secret-here
spring.security.oauth2.client.registration.my-oauth2-client.authorization-grant-type=client_credentials  ## only client_credentials grant type is supported

##spring.security.oauth2.client.provider.<provider-id>.token-uri=<token-uri>
spring.security.oauth2.client.provider.my-auth-server.token-uri=replace-token-uri-here

solace.java.host=tcps://localhost:55443  ## OATUH2 authentication scheme requires a secure connection to the broker
solace.java.msgVpn=replace-msgVpn-here
solace.java.oauth2ClientRegistrationId=my-oauth2-client ## Refers to the Spring OAuth2 client registration-id defined above
solace.java.apiProperties.AUTHENTICATION_SCHEME=AUTHENTICATION_SCHEME_OAUTH2
```

### Customizing OAuth2 Token Injection and Token Refresh

The Solace Java API OAuth2 authentication scheme supports customizing the OAuth2 token injection and
token refresh.

Create your custom implementation of
the [SolaceSessionOAuth2TokenProvider](src/main/java/com/solacesystems/jcsmp/SolaceSessionOAuth2TokenProvider.java)
interface to injection initial token.
Refer [DefaultSolaceSessionOAuth2TokenProvider](src/main/java/com/solacesystems/jcsmp/DefaultSolaceSessionOAuth2TokenProvider.java)
for sample implementation.

Similarly, create your custom implementation of
the [SolaceOAuth2SessionEventHandler](src/main/java/com/solacesystems/jcsmp/SolaceOAuth2SessionEventHandler.java)
interface to refresh token.
Refer [DefaultSolaceOAuth2SessionEventHandler](src/main/java/com/solacesystems/jcsmp/DefaultSolaceOAuth2SessionEventHandler.java)
for sample implementation.


## Resources

For more information about Spring Boot Auto-Configuration and Starters try these resources:

- [Spring Docs - Spring Boot Auto-Configuration](//docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#using-boot-auto-configuration)
- [Spring Docs - Developing Auto-Configuration](//docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#boot-features-developing-auto-configuration)
- [GitHub Tutorial - Master Spring Boot Auto-Configuration](//github.com/snicoll-demos/spring-boot-master-auto-configuration)

For more information about Solace technology in general please visit these resources:

- The [Solace Developer Portal](//dev.solace.com)
- Understanding [Solace technology.](//dev.solace.com/tech/)
- Ask the [Solace community](//dev.solace.com/community/).