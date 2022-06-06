# ZenWave Code Generator

> :warning: Work in progress and not ready for use.

ZenWave Code Generator is a configurable and extensible code generator tool for **Domain Driven Design (DDD)** and **API-First** that can generate code from a mix of different models including:

- JHipster Domain Language (JDL)
- AsyncAPI
- OpenAPI

**Table of Contents:**

- [ZenWave Code Generator](#zenwave-code-generator)
  - [Building from source](#building-from-source)
  - [Jbang Instalation](#jbang-instalation)
  - [Available Plugins](#available-plugins)
    - [AsyncAPI](#asyncapi)
      - [Spring Cloud Streams 3](#spring-cloud-streams-3)
    - [OpenAPI](#openapi)
      - [Spring WebTestClient](#spring-webtestclient)
    - [JDL](#jdl)
      - [JDL Server Entities (WIP)](#jdl-server-entities-wip)
      - [JDL Reverse Engineering from Java Classes](#jdl-reverse-engineering-from-java-classes)
      - [JDL To OpenAPI](#jdl-to-openapi)
      - [OpenAPI to JDL](#openapi-to-jdl)

## Building from source

You will need to build first one supporting project (jdl-jvm) that have not been published yet to maven central.

```shell
git clone https://github.com/ZenWave360/jdl-jvm.git && cd jdl-jvm && mvn clean install
git clone https://github.com/ZenWave360/zenwave-code-generator.git && cd zenwave-code-generator && mvn clean install
```

Now you can install with JBang.

## Jbang Instalation

You can use JBang to install the ZenWave Code Generator as a [JBang alias](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html) with the following command:

```shell
jbang alias add --name=zw \
    -m=io.zenwave360.generator.Main \
    --repos=https://oss.sonatype.org/content/repositories/snapshots \
    --deps=\
io.github.zenwave360.zenwave-code-generator.plugins:asyncapi-spring-cloud-streams3:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:openapi-spring-webtestclient:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-entities:0.0.1-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-to-openapi:0.0.1-SNAPSHOT \
    io.github.zenwave360:zenwave-code-generator-cli:0.0.1-SNAPSHOT
```

You can include any custom plugin in as `--deps` option.

> > NOTE: there is a [know bug](https://github.com/jbangdev/jbang/issues/1367) so after adding your jbang alias you may need to edit `$HOME/.jbang/jbang-catalog.json` and replace _repositories_ with _dependencies_ for your alias entry.

## Available Plugins

### AsyncAPI

#### Spring Cloud Streams 3

Generates strongly typed java code (Producer and Consumers) for Spring Cloud Streams 3 from AsyncAPI specification.

It supports:

- Imperative and Reactive styles
- Exposing your DTOs, Spring Messages or Kafka KStreams as parameter types.
- All message formats supported by AsyncAPI specification: AsyncAPI schema (inline), JSON Schema (external files) and Avro (external files).

> NOTE: some templates/combinations are still WIP

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

#### JDL Server Entities (WIP)

Aims to generate a complete Architecture based on Domain models expressed in JDL.

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLEntitiesConfigurationPreset \
    specFile=entities-model.jdl targetFolder=target/out
```

#### JDL Reverse Engineering from Java Classes

If starting with legacy project, you can reverse engineer JDL from Java entity classes. JPA and MongoDB are supported.

It requires access to your project classpath so you can just paste the following code on any test class or main method:

```java
String jdl = new JavaToJDLGenerator()
    .withPackageName("io.zenwave360.generator.jpa2jdl")
    .withPersistenceType(JavaToJDLGenerator.PersistenceType.JPA)
    .generate();
System.out.println(jdl);
```

```java
String jdl = new JavaToJDLGenerator()
    .withPackageName("io.zenwave360.generator.mongodb2jdl")
    .withPersistenceType(JavaToJDLGenerator.PersistenceType.MONGODB)
    .generate();
System.out.println(jdl);
```

#### JDL To OpenAPI

Generate OpenAPI schemas from JDL entities:

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIConfigurationPreset \
    specFile=entities-model.jdl targetFolder=target/out targetFile=openapi.yml
cat target/out/openapi.yml
```

#### OpenAPI to JDL

Reverse engineer JDL entities from OpenAPI schemas:

```shell
jbang zw -p io.zenwave360.generator.plugins.OpenAPIToJDLConfigurationPreset \
    specFile=openapi.yml targetFolder=target/out targetFile=entities.jdl
cat target/out/entities.jdl
```
