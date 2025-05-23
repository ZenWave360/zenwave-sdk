# ZenWave SDK

<p align="center"  markdown="1">
  <img src="docs/logos/code-generator-logo-dark.svg#gh-dark-mode-only" alt="ZW> Code Generator" />
  <img src="docs/logos/code-generator-logo-light.svg#gh-light-mode-only" alt="ZW> Code Generator" />
</p>

> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
![General Availability](https://img.shields.io/badge/lifecycle-GA-green)
[![build](https://github.com/ZenWave360/zenwave-sdk/workflows/Build%20and%20Publish%20Maven%20Snapshots/badge.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/publish-maven-snapshots.yml)
[![coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/jacoco.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/publish-maven-central.yml)
[![branches coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/branches.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/publish-maven-central.yml)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

> **Note**: Starting with version 2.0.0, the Maven `groupId` has changed to `io.zenwave360`. The code remains fully compatible.
> You can find [Previous Releases Documentation](https://github.com/ZenWave360/zenwave-sdk/tree/1.7.x?tab=readme-ov-file#available-plugins) here.

ZenWave SDK is a configurable and extensible toolkit for **Domain Driven Design (DDD)** and **API-First** that can generate code from a mix of different models including:

- ZDL Domain Language
- AsyncAPI
- OpenAPI

**Table of Contents:**

- [ZenWave SDK](#zenwave-sdk)
  - [Jbang Instalation](#jbang-instalation)
  - [Features / Roadmap](#features--roadmap)
  - [Building from source](#building-from-source)
  - [Usage](#usage)
  - [Available Plugins](#available-plugins)
- [ZenWave SDK Documentation](#zenwave-sdk-documentation)

## JBang Instalation

The easiest way to install ZenWave SDK is as a [JBang alias](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html):

```shell
jbang alias add --fresh --force --name=zw release@zenwave360/zenwave-sdk
```

or if you prefer to use the latest **snapshot** versions:

```shell
jbang alias add --fresh --force --name=zw snapshots@zenwave360/zenwave-sdk
```

or if you prefer to use the _next_ experimental/unstable **snapshot** versions:

```shell
jbang alias add --fresh --force --name=zw next@zenwave360/zenwave-sdk
```

If you plan to use **custom plugins** you will need to use the command in the following format:

```shell
jbang alias add --name=zw  --force \
    -m=io.zenwave360.sdk.Main \
    --repos=mavencentral,snapshots=https://s01.oss.sonatype.org/content/repositories/snapshots \
    --deps=\
org.slf4j:slf4j-simple:1.7.36,\
io.zenwave360.sdk.plugins:asyncapi-spring-cloud-streams3:1.7.1,\
io.zenwave360.sdk.plugins:asyncapi-jsonschema2pojo:1.7.1,\
io.zenwave360.sdk.plugins:openapi-spring-webtestclient:1.7.1,\
io.zenwave360.sdk.plugins:backend-application-default:1.7.1,\
io.zenwave360.sdk.plugins:zdl-to-openapi:1.7.1,\
io.zenwave360.sdk.plugins:zdl-to-asyncapi:1.7.1,\
io.zenwave360.sdk.plugins:jdl-to-asyncapi:1.7.1,\
io.zenwave360.sdk.plugins:zdl-to-markdown:1.7.1,\
io.zenwave360.sdk.plugins:openapi-controllers:1.7.1
    io.zenwave360.sdk:zenwave-sdk-cli:1.7.1
```

You can include any **custom plugin** jars in the `--deps` option.

JBang will use you maven settings for repository resolution, but you can also specify a custom maven repository in the `--repos` option.

## Features / Roadmap

- [x] ZenWave SDK CLI
  - [x] Default parsers, processors, plugins, templating, formatters, writers, etc... for AsyncAPI, OpenAPI and JDL.
  - [x] Help command: detailed, json, markdown, list of available plugins
  - [x] Fork existing (custom or standard) plugin command
- [x] ZenWave SDK Maven Plugin
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
$ jbang zw -h list

Usage: <main class> [-f] [-h[=<helpFormat>]] [-p[=<pluginClass>]]
                    [<String=Object>...]
      [<String=Object>...]
  -f, --force                Force overwrite
  -h, --help[=<helpFormat>]  Help with output format
  -p, --plugin[=<pluginClass>]
                             Plugin Class or short-code
INFO Reflections - Reflections took 566 ms to scan 59 urls, producing 2513 keys and 13329 values
ZW> SDK (2.0.0)

Available plugins:

BackendApplicationDefaultPlugin:    Generates a full backend application using the provided 'layout' property (2.0.0)
ZDLToOpenAPIPlugin:                 Generates a draft OpenAPI definitions from your ZDL entities and services. (2.0.0)
ZDLToAsyncAPIPlugin:                Generates a draft AsyncAPI file with events from your ZDL services. (2.0.0)
OpenAPIControllersPlugin:           Generates implementations based on ZDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces. (2.0.0)
AsyncApiJsonSchema2PojoPlugin:      Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files. (2.0.0)
SpringCloudStreams3Plugin:          Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI (2.0.0)
SpringCloudStreams3AdaptersPlugin:  Generates Spring Cloud Streams Consumers from AsyncAPI definitions. (2.0.0)
SpringWebTestClientPlugin:          Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification. (2.0.0)
OpenAPIKaratePlugin:                Generates test for KarateDSL based on OpenAPI specification. (2.0.0)
JDLToAsyncAPIPlugin:                Generates a full AsyncAPI definitions for CRUD operations from JDL models (2.0.0)
ZdlToJsonPlugin:                    Prints to StdOut ZDL Model as JSON (2.0.0)
OpenAPIToJDLPlugin:                 Generates JDL model from OpenAPI schemas (2.0.0)
ZdlToMarkdownPlugin:                Generates Markdown glossary from Zdl Models (2.0.0)


Use: "jbang zw -p <plugin | short-code> -h" to get help on a specific plugin
```

NOTE: it will list any available plugin, standard or custom, inside any of these root java packages "io", "com" or "org".

And get help for a given plugin:

```shell
jbang zw --help -p <pluginConfigClass>
```

You can add choose a help format from the following: help, detailed, markdown, or list:

```shell
jbang zw --help markdown -p <pluginConfigClass>
```

## Available Plugins

Refer to individual plugin's documentation for more information:

| **Plugin**                                                                               | **Description**                     | **Model Types**            |
|------------------------------------------------------------------------------------------|-------------------------------------|----------------------------|
| [Backend Application Default](./plugins/backend-application-default/README.md)           | Backend Application Default         | ZDL                        |
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO        | AsyncAPI, JsonSchema       |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3  | AsyncAPI, AVRO, JsonSchema |
| [OpenAPI Controllers](./plugins/openapi-controllers/README.md)                           | JDL OpenAPI Controllers             | OpenAPI, ZDL               |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient     | OpenAPI                    |
| [ZDL to OpenAPI](./plugins/zdl-to-openapi/README.md)                                     | ZDL to OpenAPI and OpenAPI to ZDL   | ZDL, OpenAPI               |
| [ZDL to AsyncAPI](./plugins/zdl-to-asyncapi/README.md)                                   | ZDL to AsyncAPI                     | ZDL, AsyncAPI              |
| [ZDL to Markdown](./plugins/zdl-to-markdown/README.md)                                   | ZDL to Markdown                     | ZDL                        |
| [Java 2 JDL Reverse Engineering](./plugins/java-to-jdl/README.md)                        | Java 2 JDL Reverse Engineering      | Java, JDL                  |
| [Java 2 AsyncAPI Reverse Engineering](./plugins/java-to-asyncapi/README.md)              | Java 2 AsyncAPI Reverse Engineering | Java                       |
| [MCP Server](./plugins/zenwave-mcp-server/README.md)                                     | ZenWave MCP Server                  | ZDL                        |

# ZenWave SDK Documentation

Please refer to the [documentation](https://zenwave360.github.io/zenwave-sdk/) website for more information.
