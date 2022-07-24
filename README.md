# ZenWave Code Generator

> :warning: Work in progress.

ZenWave Code Generator is a configurable and extensible code generator tool for **Domain Driven Design (DDD)** and **API-First** that can generate code from a mix of different models including:

- JHipster Domain Language (JDL)
- AsyncAPI
- OpenAPI

**Table of Contents:**

- [ZenWave Code Generator](#zenwave-code-generator)
  - [Jbang Instalation](#jbang-instalation)
  - [Building from source](#building-from-source)
  - [Available Plugins](#available-plugins)

## Jbang Instalation

You can use JBang to install the ZenWave Code Generator as a [JBang alias](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html) with the following command:

```shell
jbang alias add --name=zw \
    -m=io.zenwave360.generator.Main \
    --repos=mavencentral,snapshots=https://s01.oss.sonatype.org/content/repositories/snapshots \
    --deps=\
org.slf4j:slf4j-simple:1.7.36,\
io.github.zenwave360.zenwave-code-generator.plugins:asyncapi-spring-cloud-streams3:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:asyncapi-jsonschema2pojo:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:openapi-spring-webtestclient:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-backend-application-default:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-to-openapi:0.0.1-SNAPSHOT \
io.github.zenwave360.zenwave-code-generator.plugins:jdl-openapi-controllers:0.0.1-SNAPSHOT \
    io.github.zenwave360:zenwave-code-generator-cli:0.0.1-SNAPSHOT
```

You can include any custom plugin in as `--deps` option.

## Building from source

```shell
git clone https://github.com/ZenWave360/zenwave-code-generator.git
cd zenwave-code-generator
mvn clean install
```

## Available Plugins

| **Plugin**                                                                               | **Description**                    | **Model Types**      |
|------------------------------------------------------------------------------------------|------------------------------------|----------------------|
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO       | AsyncAPI, JsonSchema |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3 | AsyncAPI             |
| [JDL Backend Application Default](./plugins/jdl-backend-application-default/README.md)   | JDL Backend Application Default    | JDL                  |
| [JDL OpenAPI Controllers](./plugins/jdl-openapi-controllers/README.md)                   | JDL OpenAPI Controllers            | OpenAPI, JDL         |
| [JDL to OpenAPI](./plugins/jdl-to-openapi/README.md)                                     | JDL to OpenAPI and OpenAPI to JDL  | JDL, OpenAPI         |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient    | OpenAPI              |
