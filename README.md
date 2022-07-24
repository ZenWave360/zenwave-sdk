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

### AsyncAPI

#### Spring Cloud Streams 3

Generates strongly typed java code (Producer and Consumers) for Spring Cloud Streams 3 from AsyncAPI specification.

It supports:

- Imperative and Reactive styles
- Exposing your DTOs, Spring Messages or Kafka KStreams as parameter types.
- All message formats supported by AsyncAPI specification: AsyncAPI schema (inline), JSON Schema (external files) and Avro (external files).

> NOTE: some templates/combinations are still WIP

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringCloudStream3Configuration \
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
jbang zw -p io.zenwave360.generator.plugins.SpringWebTestClientConfiguration \
    specFile=openapi.yml targetFolder=target/out \
    apiPackage=io.example.integration.test.api \
    modelPackage=io.example.integration.test.api.model \
    groupBy=<SERVICE | OPERATION | PARTIAL> \
    operationIds=<comma separated or empty for all> \
    statusCodes=<comma separated or empty for default>
```

### JDL

#### JDL Server Backend Application

Aims to generate a complete Architecture based on Domain models expressed in JDL.

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration \
    specFile=entities-model.jdl \
    basePackage=io.zenwave360.example \
    persistence=mongodb \
    style=imperative \
    targetFolder=. \
    -h -f MARKDOWN
```

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFile` | OpenAPI file to parse |  |   |
| `specFiles` | JDL files to parse |  |   |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) |  |   |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated) |  |   |
| `paginatedDtoItemsJsonPath` | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. |  |   |
| `dtoToEntityNameMap` | Maps openapi dtos to jdl entity names |  |   |
| `controllersPackage` | The package to generate REST Controllers |  |   |
| `entitiesPackage` | Package where your domain entities are |  |   |
| `inboundDtosPackage` | Package where your inbound dtos are |  |   |
| `servicesPackage` | Package where your domain services/usecases interfaces are |  |   |
| `inputDTOSuffix` | Suffix for CRUD operations DTOs (default: Input) |  |   |
| `entityDTOSuffix` | Suffix for (output) entities DTOs (default: empty to use the entity itself) |  |   |
| `criteriaDTOSuffix` | Suffix for search criteria DTOs (default: Criteria) |  |   |
| `searchDTOSuffix` | Suffix for elasticsearch document entities (default: Document) |  |   |
| `style` | ProgrammingStyle imperative|reactive default: imperative |  | imperative, reactive  |
| `basePackage` | Applications base package |  |   |
| `openApiApiPackage` | The package to used by OpenAPI-Generator for generated api objects/classes |  |   |
| `openApiModelPackage` | The package to used by OpenAPI-Generator for generated model objects/classes |  |   |
| `openApiModelNamePrefix` | Sets the prefix for model enums and classes used by OpenAPI-Generator |  |   |
| `openApiModelNameSuffix` | Sets the suffix for model enums and classes used by OpenAPI-Generator |  |   |
| `role` | Project role: PROVIDER\|CLIENT |  | PROVIDER, CLIENT  |
| `operationIds` | OpenAPI operationIds to generate code for |  |   |
| `statusCodes` | Status codes to generate code for (default: 200, 201, 202 and 400 |  |   |
| `targetFolder` | Target folder to generate code to. If left empty, it will print to stdout. |  |   |
| `jdlFile` | JDL file to parse |  |   |



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
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration \
    specFile=entities-model.jdl targetFolder=target/out targetFile=openapi.yml
cat target/out/openapi.yml
```

#### OpenAPI to JDL

Reverse engineer JDL entities from OpenAPI schemas:

```shell
jbang zw -p io.zenwave360.generator.plugins.OpenAPIToJDLConfiguration \
    specFile=openapi.yml targetFolder=target/out targetFile=entities.jdl
cat target/out/entities.jdl
```
