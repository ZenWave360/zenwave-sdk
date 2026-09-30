# ZenWave SDK

<p align="center"  markdown="1">
  <img src="docs/logos/code-generator-logo-dark.svg#gh-dark-mode-only" alt="ZW> Code Generator" />
  <img src="docs/logos/code-generator-logo-light.svg#gh-light-mode-only" alt="ZW> Code Generator" />
</p>

> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
![General Availability](https://img.shields.io/badge/lifecycle-GA-green)
[![build](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/publish-maven-snapshots.yml/badge.svg?branch=develop)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/publish-maven-snapshots.yml)
[![coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/jacoco.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/main.yml)
[![branches coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/branches.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/main.yml)
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
  - [Available Plugins](#available-plugins)
  - [Changelog](#changelog)
  - [Features / Roadmap](#features--roadmap)
  - [Building from source](#building-from-source)
  - [Usage](#usage)

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

If you plan to use **custom plugins** you can create your own [jbang-catalog.json](jbang-catalog.json) in the current folder where you want to use `jbang zw`or perform a custom instalation with a command in the following format:

```shell
jbang alias add --name=zw  --force \
    -m=io.zenwave360.sdk.Main \
    --repos=mavencentral,snapshots=https://s01.oss.sonatype.org/content/repositories/snapshots \
    --deps=\
org.slf4j:slf4j-simple:1.7.36,\
io.zenwave360.sdk.plugins:asyncapi-spring-cloud-streams3:2.4.0,\
io.zenwave360.sdk.plugins:asyncapi-jsonschema2pojo:2.4.0,\
io.zenwave360.sdk.plugins:avro-schema-compiler:2.4.0,\
org.apache.avro:avro-compiler:1.12.0,\
io.zenwave360.sdk.plugins:asyncapi-generator:2.4.0,\
io.zenwave360.sdk.plugins:openapi-spring-webtestclient:2.4.0,\
io.zenwave360.sdk.plugins:openapi-karate:2.4.0,\
io.zenwave360.sdk.plugins:backend-application-default:2.4.0,\
io.zenwave360.sdk.plugins.customizations:kotlin-backend-application:2.4.0,\
io.zenwave360.sdk.plugins:zdl-to-openapi:2.4.0,\
io.zenwave360.sdk.plugins:zdl-to-asyncapi:2.4.0,\
io.zenwave360.sdk.plugins:jdl-to-asyncapi:2.4.0,\
io.zenwave360.sdk.plugins:zdl-to-markdown:2.4.0,\
io.zenwave360.sdk.plugins:openapi-controllers:2.4.0
    io.zenwave360.sdk:zenwave-sdk-cli:2.4.0
```

## Available Plugins

Refer to individual plugin's documentation for more information:

| **Plugin**                                                                               | **Description**                                                  | **Model Types**          |
|------------------------------------------------------------------------------------------|------------------------------------------------------------------|--------------------------|
| [Backend Application Default](./plugins/backend-application-default/README.md)           | Backend Application Default                                      | ZDL                      |
| [AsyncAPI Generator](./plugins/asyncapi-generator/README.md)                             | Generates full Java SDKs from AsyncAPI/Avro/JsonSchema           | AsyncAPI, Avro, JsonSchema |
| [Avro Schema Generator](./plugins/avro-schema-compiler/README.md)                        | AsyncAPI JSON Schema to POJO                                     | Avro                     |
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO                                     | AsyncAPI, JsonSchema     |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3 (old, see AsyncAPI Generator) | AsyncAPI, AVRO, JsonSchema |
| [AsyncAPI to Terraform](./plugins/asyncapi-ops/README.md)                                | AsyncAPI to Terraform for Kafka and Schema Registry  (LAB)       | AsyncAPI, AVRO |
| [EventCatalog Generator](./plugins/event-catalog-generator/README.md)                              | EventCatalog Generator from OpenAPI, AsyncAPI, Markdown, ZDL     | OpenAPI, AsyncAPI, Markdown, ZDL |
| [OpenAPI Controllers](./plugins/openapi-controllers/README.md)                           | ZDL OpenAPI Controllers                                          | OpenAPI, ZDL             |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient                                  | OpenAPI                  |
| [ZDL to OpenAPI](./plugins/zdl-to-openapi/README.md)                                     | ZDL to OpenAPI and OpenAPI to ZDL                                | ZDL, OpenAPI             |
| [ZDL to AsyncAPI](./plugins/zdl-to-asyncapi/README.md)                                   | ZDL to AsyncAPI                                                  | ZDL, AsyncAPI            |
| [ZDL to Markdown](./plugins/zdl-to-markdown/README.md)                                   | ZDL to Markdown                                                  | ZDL                      |
| [Java 2 JDL Reverse Engineering](./plugins/java-to-jdl/README.md)                        | Java 2 JDL Reverse Engineering                                   | Java, JDL                |
| [Java 2 AsyncAPI Reverse Engineering](./plugins/java-to-asyncapi/README.md)              | Java 2 AsyncAPI Reverse Engineering                              | Java                     |
| [MCP Server](./plugins/zenwave-mcp-server/README.md)                                     | ZenWave MCP Server                                               | ZDL                      |

# ZenWave SDK Documentation

Please refer to the [documentation](https://zenwave360.github.io/zenwave-sdk/) website for more information.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for an index of published release notes.

## Features / Roadmap

This is the original roadmap, fully implemented some years/versions ago. It's kept here as a testimony of the fundational features included in ZenWave SDK.

- [x] ZenWave SDK CLI
  - [x] Default parsers, processors, plugins, templating, formatters, writers, etc... for AsyncAPI, OpenAPI and JDL.
  - [x] Help command: detailed, json, markdown, list of available plugins
  - [x] Fork existing (custom or standard) plugin command
- [x] ZenWave SDK Maven Plugin
- [x] Standard Plugins
  - [x] ZDL Backend Application (multiple architectures: hexagonal, layered, simple domain, modular monolith...)
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
  - [x] ZDL OpenAPI Controllers
  - [x] OpenAPI to Spring WebTestClient
  - [x] AsyncAPI Spring Cloud Streams3
    - [x] Consumer and Producer. Imperative and Reactive.
      - [x] Business Exceptions Dead Letter Queues Routing
    - [x] Producer with Transactional Outbox pattern
      - [x] For MongoDB
      - [x] For JDBC
    - [x] Enterprise Envelop Pattern
    - [x] Automatically fill headers at runtime from payload paths, tracing-id supplier...
  - [x] ZDL to Specs
    - [x] ZDL to OpenAPI
    - [x] ZDL to AsyncAPI
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

### Lint ZDL models

Lint is a regular plugin, with deterministic output for terminals, agents, and CI. One ZDL file per run:

```shell
jbang zw -p Lint zdlFile=<file.zdl> [format=text|json|sarif] [output=<file>] [strict=true]                  [root=<dir>] [linters=<Linter,...>]
```

Run `jbang zw -p Lint -h` for all options; like any plugin they can also be set in a zenwave-scripts `.zw` plugin block.

Text output uses `file:line:column: severity ruleId message`. JSON uses a versioned envelope
(`schemaVersion`, `tool`, `summary`, `diagnostics`, and `toolFailures` when a linter failed) and SARIF output
conforms to SARIF 2.1.0 with `%SRCROOT%` locations, `rules[]` derived from the reported rule ids and stable
partial fingerprints. Positions are one-based; paths are relative to the working directory, or to `root`.
The report goes to stdout, or to `output=` as UTF-8 (PowerShell 5.1 `>` writes UTF-16).

Exit codes (CLI-wide): `0` success; `1` findings (errors, or warnings with `strict=true`); `2` tool failure
(invalid arguments, unreadable input, a failing linter, or any other exception). Plugins signal a code by throwing
`ZenWaveException(message, exitCode)`.

When run from the command line, `System.out` is redirected to stderr during plugin execution, so logs never
pollute stdout: only deliberate results (the lint report, `ZdlToJson`, template stdout output) reach it.

`linters` is an ordered list of `io.zenwave360.sdk.lint.Linter` implementations (fully qualified names, or short
names of built-ins such as `ZdlProblems`), loaded from the project classpath so custom linters can ship in jars
added with `--deps`. The default set is: `ZdlProblemsLinter` (syntax and semantic problems, including
lifecycle/transition states), `ZdlReferencesLinter` (unresolvable `zdl`/`asyncapi` clients), `ZdlCallsLinter`
(`@calls` targets) and `ZdlListenerLinter` (`@listener` events, mixed `@asyncapi`/`@listener` bindings and
required-input data-lineage warnings). Syntax errors are reported as a single problem and semantic linters are
skipped after them. Presets are plugin subclasses of `LintPlugin` with a different default `linters` list.

Use the following jbang format:

```shell
jbang zw -p <pluginConfigClass or short-code> optionName=value optionName2=value
```

You can get a list of all available plugins:

```shell
 jbang zw -h list
Usage: <main class> [-f] [-h[=<helpFormat>]] [-p[=<pluginClass>]] [-d=<deps>[,
                    <deps>...]]... [-r=<repos>[,<repos>...]]...
                    [<String=Object>...]
      [<String=Object>...]
  -d, --deps=<deps>[,<deps>...]
                             Dependencies to include in classpath
  -f, --force                Force overwrite
  -h, --help[=<helpFormat>]  Help with output format
  -p, --plugin[=<pluginClass>]
                             Plugin Class or short-code
  -r, --repos=<repos>[,<repos>...]
                             Repositories to search for extra dependencies
INFO Reflections - Reflections took 3725 ms to scan 97 urls, producing 8936 keys and 63781 values
ZW> SDK (2.4.0)

Available plugins:

io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin: Generates a full backend application using the provided 'layout' property (2.4.0)
io.zenwave360.sdk.plugins.SpringCloudStreams3AdaptersPlugin: Generates Spring Cloud Streams Consumers from AsyncAPI definitions. (2.4.0)
io.zenwave360.sdk.plugins.JDLToAsyncAPIPlugin: Generates a full AsyncAPI definitions for CRUD operations from JDL models (2.4.0)
io.zenwave360.sdk.plugins.AsyncAPIGeneratorPlugin: Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI with Avro and JSON DTOs (2.4.0)
io.zenwave360.sdk.plugins.ZdlToJsonPlugin: Prints to StdOut ZDL Model as JSON (2.4.0)
io.zenwave360.sdk.plugins.SpringWebTestClientPlugin: Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification. (2.4.0)
io.zenwave360.sdk.plugins.AsyncApiJsonSchema2PojoPlugin: Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files. (2.4.0)
io.zenwave360.sdk.plugins.OpenAPIControllersPlugin: Generates implementations based on ZDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces. (2.4.0)
io.zenwave360.sdk.plugins.OpenAPIToJDLPlugin: Generates JDL model from OpenAPI schemas (2.4.0)
io.zenwave360.sdk.plugins.SpringCloudStreams3Plugin: Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI (2.4.0)
io.zenwave360.sdk.plugins.OpenAPIKaratePlugin: Generates test for KarateDSL based on OpenAPI specification. (2.4.0)
io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin: Generates a draft OpenAPI definitions from your ZDL entities and services. (2.4.0)
io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin: Generates Markdown glossary from Zdl Models (2.4.0)
io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin: Generates a draft AsyncAPI file with events from your ZDL services. (2.4.0)
io.zenwave360.sdk.plugins.AvroSchemaGeneratorPlugin: Generates Java classes from Avro schemas using Avro Compiler. (2.4.0)


Use: "jbang zw -p <plugin | short-code> -h" to get help on a specific plugin


Use: "jbang zw -p <plugin | short-code> -h" to get help on a specific plugin
```

You can include any **custom plugin** jars in the `--deps` option.

`--deps` is resolved by ZenWave using JBang's Maven resolver. Extra repositories come from, in order:

1. `zw --repos` / `-r` (`id=url` or a JBang alias such as `central`)
2. Silent JBang defaults: `run.repos` or `repos` in `jbang.properties` (project directory walking up, then `~/.jbang/jbang.properties`)
3. Maven Central and Sonatype snapshots

`~/.m2/settings.xml` is used for **credentials** (matching the repository id), not for extra repository URLs.

To use GitHub Packages without passing `--repos` on every command:

```shell
jbang config set run.repos central,github=https://maven.pkg.github.com/OWNER/REPO
```

Or drop a project-local `jbang.properties` next to your `.zw` scripts:

```properties
run.repos=central,github=https://maven.pkg.github.com/OWNER/REPO
```

GitHub Packages requires authentication even for public packages. Use a `~/.m2/settings.xml` `<server>` whose `<id>` matches the repo id (`github` in the examples above), or `GITHUB_TOKEN` / `~/.netrc` host auth for `maven.pkg.github.com`.


NOTE: it will list any available plugin, standard or custom, inside any of these root java packages "io", "com" or "org".

And get help for a given plugin:

```shell
jbang zw --help -p <pluginConfigClass>
```

You can add choose a help format from the following: help, detailed, markdown, or list:

```shell
jbang zw --help markdown -p <pluginConfigClass>
```
