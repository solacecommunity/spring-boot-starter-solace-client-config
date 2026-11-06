# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 3.1.5 - 2026-11-06
### Added
- `SSL_PRIVATE_KEY` accepts an EC private key besides an RSA one, so ECDSA client certificates work

## 3.1.4 - 2026-11-05
### Changed
- Spring Boot 4.1.2
- The optional `SolaceSessionOAuth2TokenProvider` is injected through an `ObjectProvider` instead of the deprecated `org.springframework.lang.Nullable`
- Test and build dependencies: gson 2.15.0, okhttp 5.5.1, swagger-core, swagger-annotations and swagger-models 3.0.0, swagger-codegen-maven-plugin 3.0.83

## 3.1.3 - 2026-10-08
### Changed
- Dependency update

## 3.1.2 - 2026-09-10
### Changed
- Dependency update

## 3.1.1 - 2026-08-04
### Changed
- Dependency update

## 3.1.0 - 2026-07-19
### Changed
- Dependency update (spring boot 4.1)

## 3.0.3 - 2026-04-21
### Changed
- Dependency update

## 3.0.2 - 2026-02-20
### Changed
- Dependency update

## 3.0.1 - 2026-02-16
### Changed
- Update to Spring Boot 4

## 2.0.0 - 2025-12-10
### Changed
- Dependency update / spring security update.

### Added
- Integrate the functionality of solace-java-spring-boot-autoconfigure. To no longer depend on the slow update cycle of solace.

## 1.1.1 - 1.1.3
Faulty versions, dont use!

## 1.1.0 - 2025-08-15
### Changed
 - Dependency update / spring security update.

### Added
 - Log messages if the certificate is about to expire

## 1.0.0 - 2023-09-27
### Added
 - Initial version
