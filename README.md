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
  - [Usage](#usage)
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

## Usage

Once installed through the JBang alias, you can use the ZenWave Code Generator by running a command in the following format:

```shell
jbang zw -p <pluginConfigClass> optionName=value optionName2=value
```

You can get a list of all available plugins:

```shell
$ jbang zw -h -f list
INFO Reflections - Reflections took 428 ms to scan 44 urls, producing 2493 keys and 14406 values
Available plugins:

jsonschema2pojo                io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoConfiguration: Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files
jdl-backend-application-default io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration: Generates a full backend application using a flexible hexagonal architecture
jdl-to-openapi                 io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration: Generates a full OpenAPI definitions for CRUD operations from JDL models
openapi-to-jdl                 io.zenwave360.generator.plugins.OpenAPIToJDLConfiguration: Generates JDL model from OpenAPI schemas
spring-cloud-streams3          io.zenwave360.generator.plugins.SpringCloudStream3Configuration: Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI
spring-webtestclient           io.zenwave360.generator.plugins.SpringWebTestClientConfiguration: Generates spring WebTestClient tests from OpenAPI defined endpoints.
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
|------------------------------------------------------------------------------------------|------------------------------------|----------------------------|
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO       | AsyncAPI, JsonSchema       |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3 | AsyncAPI, AVRO, JsonSchema |
| [JDL Backend Application Default](./plugins/jdl-backend-application-default/README.md)   | JDL Backend Application Default    | JDL                        |
| [Java 2 JDL Reverse Engineering](./plugins/java-to-jdl/README.md)                        | Java 2 JDL Reverse Engineering     | Java, JDL                  |
| [JDL OpenAPI Controllers](./plugins/jdl-openapi-controllers/README.md)                   | JDL OpenAPI Controllers            | OpenAPI, JDL               |
| [JDL to OpenAPI](./plugins/jdl-to-openapi/README.md)                                     | JDL to OpenAPI and OpenAPI to JDL  | JDL, OpenAPI               |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient    | OpenAPI                    |

