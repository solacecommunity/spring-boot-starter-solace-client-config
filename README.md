# spring-boot-starter-solace-client-config

This starter enables the Solace configuration to use certificates and private keys in the PEM format. As PEM is just a text format,
it can be easily passed to the application configuration directly from an environment variable or configuration files such as `.yaml` or
`.properties`. Loading certificates and private keys in the `.jks` format from the file system is no longer necessary.

## Usage

Add a dependency in your application POM:
```xml

<dependency>
    <groupId>community.solace.spring.boot</groupId>
    <artifactId>spring-boot-starter-solace-client-config</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

The PEM strings can be defined via environment variables, properties or directly in the `application.yml`.
```yaml
solace:
  java:
    apiProperties:
      AUTHENTICATION_SCHEME: AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
      SSL_CLIENT_CERT: ${SOLACE_CLIENT_CERT}
      SSL_PRIVATE_KEY: ${SOLACE_PRIVATE_KEY}
      SSL_TRUST_CERT: ${SOLACE_TRUST_ROOTS}
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
                  SSL_TRUST_CERT: ${SOLACE_TRUST_ROOTS}
```

Note, that for both configuration cases, all 4 API properties (AUTHENTICATION_SCHEME, SSL_CLIENT_CERT, SSL_PRIVATE_KEY, SSL_TRUST_CERT) are required to 
create a valid Solace broker connection configuration.
