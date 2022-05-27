# ZenWave Code Generator

> :warning: Work in progress and not ready for use.

ZenWave Code Generator is a configurable and extensible code generator tool for **Domain Driven Design (DDD)** and **API-First** that can generate code from a mix of different models including:

- JHipster Domain Language (JDL)
- AsyncAPI
- OpenAPI

**Table of Contents:**

- [ZenWave Code Generator](#zenwave-code-generator)
  - [Jbang Instalation](#jbang-instalation)
  - [Available Plugins](#available-plugins)
    - [AsyncAPI](#asyncapi)
      - [Spring Cloud Streams 3](#spring-cloud-streams-3)
    - [OpenAPI](#openapi)
      - [Spring WebTestClient](#spring-webtestclient)
    - [JDL](#jdl)
      - [JDL Entities](#jdl-entities)

## Jbang Instalation

You can use JBang to install the ZenWave Code Generator as a [JBang alias](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html) with the following command:

```shell
jbang alias add --name=zw \
    -m=io.zenwave360.generator.Main \
    --deps=io.github.zenwave360.generator.plugins:asyncapi-spring-cloud-streams3:0.0.1-SNAPSHOT,io.github.zenwave360.generator.plugins:jdl-entities:0.0.1-SNAPSHOT \
    io.github.zenwave360:zenwave-code-generator-cli:0.0.1-SNAPSHOT
```

You can include any custom plugin in the `--deps` option.

> > NOTE: there is a [know bug](https://github.com/jbangdev/jbang/issues/1367) so after adding your jbang alias you may need to edit `$HOME/.jbang/jbang-catalog.json` and replace _repositories_ with _dependencies_ for your alias entry.

## Available Plugins

### AsyncAPI

#### Spring Cloud Streams 3

Generates strongly typed java code (Producer and Consumers) for Spring Cloud Streams 3 from AsyncAPI specification.

It supports:

- Imperative and Reactive styles
- Exposing your DTOs, Spring Messages or Kafka KStreams as parameter types.
- All message formats supported by AsyncAPI specification: AsyncAPI schema (inline), JSON Schema (external files) and Avro (external files).

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringCloudStream3ConfigurationPreset \
    specFile=asyncapi.yml targetFolder=target/out \
    apiPackage=io.example.integration.test.api \
    modelPackage=io.example.integration.test.api.model \
    role=<PROVIDER | CLIENT> \
    style=<IMPERATIVE | REACTIVE>
```

### OpenAPI

#### Spring WebTestClient

Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification.

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringWebTestsClientConfigurationPreset \
    specFile=openapi.yml targetFolder=target/out \
    apiPackage=io.example.integration.test.api \
    modelPackage=io.example.integration.test.api.model \
    groupBy=<SERVICE | OPERATION | PARTIAL> \
    operationIds=<comma separated or empty for all> \
    statusCodes=<comma separated or empty for default>
```

### JDL

#### JDL Entities

Aims to generate an Hexagonal Architecture based on Domain models expressed in JDL.

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLEntitiesConfigurationPreset \
    specFile=entities-model.jdl targetFolder=target/out
```
