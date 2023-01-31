# ZenWave Code Generator

<p align="center"  markdown="1">
  <img src="docs/logos/code-generator-logo-dark.svg#gh-dark-mode-only" alt="ZW> Code Generator" />
  <img src="docs/logos/code-generator-logo-light.svg#gh-light-mode-only" alt="ZW> Code Generator" />
</p>

> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zenwave360.zenwave-sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.github.zenwave360.zenwave-sdk/zenwave-sdk)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/ZenWave360/zenwave-sdk?logo=GitHub)](https://github.com/ZenWave360/zenwave-sdk/releases)
![General Availability](https://img.shields.io/badge/lifecycle-GA-green)
[![build](https://github.com/ZenWave360/zenwave-sdk/workflows/build/badge.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/jacoco.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![branches coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/branches.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

ZenWave Code Generator is a configurable and extensible code generator tool for **Domain Driven Design (DDD)** and **API-First** that can generate code from a mix of different models including:

- JHipster Domain Language (JDL)
- AsyncAPI
- OpenAPI

**Table of Contents:**

- [ZenWave Code Generator](#zenwave-sdk)
  - [Jbang Instalation](#jbang-instalation)
  - [Features / Roadmap](#features--roadmap)
  - [Building from source](#building-from-source)
  - [Usage](#usage)
  - [Available Plugins](#available-plugins)
  - [Forking an Standard or Custom Plugin](#forking-an-standard-or-custom-plugin)
- [ZenWave Code Generator Documentation](#zenwave-sdk-documentation)

## Jbang Instalation

The easiest way to install ZenWave Code Generator is as a [JBang alias](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html):

```shell
jbang alias add --fresh --name=zw release@zenwave360/zenwave-sdk
```

or if you prefer to use the latest **snapshot** versions:

```shell
jbang alias add --fresh --name=zw zw-snapshots@zenwave360/zenwave-sdk
```

If you plan to use **custom plugins** you will need to use the command in the following format:

```shell
jbang alias add --name=zw \
    -m=io.zenwave360.generator.Main \
    --repos=mavencentral,snapshots=https://s01.oss.sonatype.org/content/repositories/snapshots \
    --deps=\
org.slf4j:slf4j-simple:1.7.36,\
io.github.zenwave360.zenwave-sdk.plugins:asyncapi-spring-cloud-streams3:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:asyncapi-jsonschema2pojo:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:openapi-spring-webtestclient:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:openapi-rest-assured:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:jdl-backend-application-default:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:jdl-to-openapi:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:jdl-to-asyncapi:1.0.0.RC1,\
io.github.zenwave360.zenwave-sdk.plugins:jdl-openapi-controllers:1.0.0.RC1 \
    io.github.zenwave360.zenwave-sdk:zenwave-sdk-cli:1.0.0.RC1
```

You can include any **custom plugin** jars in the `--deps` option.

JBang will use you maven settings for repository resolution, but you can also specify a custom maven repository in the `--repos` option.

## Features / Roadmap

- [x] ZenWave Code Generator CLI
  - [x] Default parsers, processors, plugins, templating, formatters, writers, etc... for AsyncAPI, OpenAPI and JDL.
  - [x] Help command: detailed, json, markdown, list of available plugins
  - [x] Fork existing (custom or standard) plugin command
- [x] ZenWave Code Generator Maven Plugin
- [x] Standard Plugins
  - [x] JDL Backend Application (flexible hexagonal architecture)
    - [x] Domain Entities
    - [x] Inbound
      - [x] Service Ports, DTOs, Mappers
      - [x] Implementation for CRUD operations
      - [x] Acceptance Tests: SpringData InMemory Repositories
    - [x] Outbound: SpringData Repositories, ElasticSearch... (for REST or Async see other plugins)
    - [x] Adapters:
      - [x] Spring MVC
      - [ ] ~~Spring WebFlux~~
    - [ ] Flavors
      - [x] MongoDB
        - [x] Imperative
        - [ ] ~~Reactive~~
      - [x] JPA
        - [x] Imperative
        - [ ] ~~Reactive~~
    - [x] Unit/Integration Testing
      - [x] Edge Integration Testing: partial spring-boot context for outbound adapters (with testcontainers)
      - [x] Sociable Vertical Testing: manual dependency setup with in memory infrastructure _test-doubles_
      - [x] Vertical Integration Testing: full spring-boot context for inbound adapters (with testcontainers)
  - [x] JDL OpenAPI Controllers
  - [x] OpenAPI to Spring WebTestClient
  - [x] AsyncAPI Spring Cloud Streams3
    - [x] Consumer and Producer. Imperative and Reactive.
      - [x] Business Exceptions Dead Letter Queues Routing
    - [x] Producer with Transactional Outbox pattern
      - [x] For MongoDB
      - [x] For JDBC
    - [x] Enterprise Envelop Pattern
    - [x] Automatically fill headers at runtime from payload paths, tracing-id supplier...
  - [x] JDL to Specs
    - [x] JDL to OpenAPI
    - [x] JDL to AsyncAPI
      - [x] AsyncAPI schemas
      - [x] AVRO schemas
  - [x] API Testing
    - [x] KarateDSL
      - [x] OpenAPI to Karate E2E Tests (please use [KarateIDE VSCode Extension](https://github.com/ZenWave360/karate-ide) instead)
      - [x] OpenAPI to Karate/ApiMock Stateful Mocks (please use [KarateIDE VSCode Extension](https://github.com/ZenWave360/karate-ide) and [ZenWave ApiMock](https://github.com/ZenWave360/zenwave-apimock) instead)
    - [x] OpenAPI to Spring WebTestClient
    - [x] OpenAPI to REST-assured
    - [ ] ~~OpenAPI to Pact (_postponed sine die_)~~
  - [x] Reverser Engineering
    - [x] OpenAPI 2 JDL
    - [x] Java 2 JDL
      - [x] Spring Data MongoDB annotations
      - [x] JPA annotations

## Building from source

```shell
git clone https://github.com/ZenWave360/zenwave-sdk.git
cd zenwave-sdk
mvn clean install
```

## Usage

Use the following jbang format:

```shell
jbang zw -p <pluginConfigClass or short-code> optionName=value optionName2=value
```

You can get a list of all available plugins:

```shell
$ jbang zw -h -f list
INFO Reflections - Reflections took 428 ms to scan 44 urls, producing 2493 keys and 14406 values
Available plugins:

jsonschema2pojo                io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoPlugin: Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files
jdl-backend-application-default io.zenwave360.generator.plugins.JDLBackendApplicationDefaultPlugin: Generates a full backend application using a flexible hexagonal architecture
jdl-to-openapi                 io.zenwave360.generator.plugins.JDLToOpenAPIPlugin: Generates a full OpenAPI definitions for CRUD operations from JDL models
jdl-openapi-controllers        io.zenwave360.generator.plugins.JDLOpenAPIControllersPlugin: Generates implementations based on JDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.
openapi-to-jdl                 io.zenwave360.generator.plugins.OpenAPIToJDLPlugin: Generates JDL model from OpenAPI schemas
spring-cloud-streams3          io.zenwave360.generator.plugins.SpringCloudStreams3Plugin: Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI
fork-plugin                    io.zenwave360.generator.plugins.ForkPlugin: Creates a new standalone maven module cloning an existing plugin
spring-webtestclient           io.zenwave360.generator.plugins.SpringWebTestClientPlugin: Generates spring WebTestClient tests from OpenAPI defined endpoints.
rest-assured                   io.zenwave360.generator.plugins.RestAssuredPlugin: Generates REST-assured tests from OpenAPI defined endpoints.
```

NOTE: it will list any available plugin, standard or custom, inside any of these root java packages "io", "com" or "org".

And get help for a given plugin:

```shell
jbang zw --help -p <pluginConfigClass>
```

You can add choose a help format from the following: help, detailed, markdown, or list:

```shell
jbang zw --help --help-format markdown -p <pluginConfigClass>
```

## Available Plugins

Refer to individual plugin's documentation for more information:

| **Plugin**                                                                               | **Description**                    | **Model Types**            |
|------------------------------------------------------------------------------------------|------------------------------------| -------------------------- |
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO       | AsyncAPI, JsonSchema       |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3 | AsyncAPI, AVRO, JsonSchema |
| [JDL Backend Application Default](./plugins/jdl-backend-application-default/README.md)   | JDL Backend Application Default    | JDL                        |
| [Java 2 JDL Reverse Engineering](./plugins/java-to-jdl/README.md)                        | Java 2 JDL Reverse Engineering     | Java, JDL                  |
| [JDL OpenAPI Controllers](./plugins/jdl-openapi-controllers/README.md)                   | JDL OpenAPI Controllers            | OpenAPI, JDL               |
| [JDL to OpenAPI](./plugins/jdl-to-openapi/README.md)                                     | JDL to OpenAPI and OpenAPI to JDL  | JDL, OpenAPI               |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient    | OpenAPI                    |
| [REST-Assured](./plugins/openapi-rest-assured/README.md)                                 | OpenAPI to REST-Assured            | OpenAPI                    |

## Forking an Standard or Custom Plugin

One promise of ZenWave Code Generator is to be easily extensible and adaptable to your project or your organization needs and likes.

You can always fork an existing plugin with the following command:

```shell
jbang zw -p io.zenwave360.generator.plugins.ForkPlugin -h
```

| **Option**              | **Description**                                                                     | **Type** | **Default**                                                                       | **Values** |
|-------------------------|-------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------|------------|
| `targetFolder`          |                                                                                     | String   |                                                                                   |            |
| `sourcePluginClassName` | Plugin Plugin class to fork                                                         | String   |                                                                                   |            |
| `targetPluginClassName` | New Plugin Plugin class. It will be used for class name, package and maven groupId. | String   |                                                                                   |            |
| `downloadURL`           | Download URL for the source code of original plugin in zip format                   | URL      | https://github.com/ZenWave360/zenwave-sdk/archive/refs/tags/v1.0.0.RC1.zip |            |

Example:

```shell
jbang zw -p io.zenwave360.generator.plugins.ForkPlugin \
            targetFolder=target/forked-plugin \
            sourcePluginClassName=io.zenwave360.generator.plugins.JDLBackendApplicationDefaultPlugin \
            targetPluginClassName=com.myorganization.generator.JDLBackendApplicationDefaultPluginForked
cd target/forked-plugin
mvn clean install
```

Now you can add this jar to the list of available plugins in [jbang install command](#jbang-instalation)

# ZenWave Code Generator Documentation

Please refer to the [documentation](https://zenwave360.github.io/zenwave-sdk/) website for more information.
