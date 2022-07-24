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
    - [AsyncApiJsonSchema2PojoConfiguration](#asyncapijsonschema2pojoconfiguration)
    - [SpringCloudStream3Configuration](#springcloudstream3configuration)
    - [JDLBackendApplicationDefaultConfiguration](#jdlbackendapplicationdefaultconfiguration)
    - [Java 2 JDL Reverse Engineering](#java-2-jdl-reverse-engineering)
    - [JDLOpenAPIControllersConfiguration](#jdlopenapicontrollersconfiguration)
    - [JDLToOpenAPIConfiguration](#jdltoopenapiconfiguration)
    - [OpenAPIToJDLConfiguration](#openapitojdlconfiguration)
    - [SpringWebTestClientConfiguration](#springwebtestclientconfiguration)

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
| ---------------------------------------------------------------------------------------- | ---------------------------------- | -------------------- |
| [AsyncAPI JSON Schema to POJO](./plugins/asyncapi-jsonschema2pojo/README.md)             | AsyncAPI JSON Schema to POJO       | AsyncAPI, JsonSchema |
| [AsyncAPI to Spring Cloud Streams 3](./plugins/asyncapi-spring-cloud-streams3/README.md) | AsyncAPI to Spring Cloud Streams 3 | AsyncAPI             |
| [JDL Backend Application Default](./plugins/jdl-backend-application-default/README.md)   | JDL Backend Application Default    | JDL                  |
| [Java 2 JDL Reverse Engineering](./plugins/java-to-jdl/README.md)                        | Java 2 JDL Reverse Engineering     | Java, JDL            |
| [JDL OpenAPI Controllers](./plugins/jdl-openapi-controllers/README.md)                   | JDL OpenAPI Controllers            | OpenAPI, JDL         |
| [JDL to OpenAPI](./plugins/jdl-to-openapi/README.md)                                     | JDL to OpenAPI and OpenAPI to JDL  | JDL, OpenAPI         |
| [OpenAPI to Spring WebTestClient](./plugins/openapi-spring-webtestclient/README.md)      | OpenAPI to Spring WebTestClient    | OpenAPI              |

### AsyncApiJsonSchema2PojoConfiguration

Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files

${javadoc}

**Options:**

| **Option**        | **Description**                                                                                                | **Type** | **Default**          | **Values**       |
| ----------------- | -------------------------------------------------------------------------------------------------------------- | -------- | -------------------- | ---------------- |
| `specFile`        | API Specification File                                                                                         | String   |                      |                  |
| `targetFolder`    | Target folder to generate code to. If left empty, it will print to stdout.                                     | File     |                      |                  |
| `messageNames`    | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty | List     | []                   |                  |
| `jsonschema2pojo` | JsonSchema2Pojo settings                                                                                       | Map      | {}                   |                  |
| `apiPackage`      | Java API package name                                                                                          | String   | io.example.api       |                  |
| `modelPackage`    | Java Models package name                                                                                       | String   | io.example.api.model |                  |
| `bindingTypes`    | Binding names to include in code generation. Generates code for ALL bindings if left empty                     | List     |                      |                  |
| `role`            | Project role: PROVIDER\|CLIENT                                                                                 | RoleType | PROVIDER             | PROVIDER, CLIENT |
| `operationIds`    | Operation ids to include in code generation. Generates code for ALL if left empty                              | List     | []                   |                  |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoConfiguration --help
```

### SpringCloudStream3Configuration

Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI

${javadoc}

**Options:**

| **Option**      | **Description**                                                                            | **Type**         | **Default**          | **Values**           |
| --------------- | ------------------------------------------------------------------------------------------ | ---------------- | -------------------- | -------------------- |
| `specFile`      | API Specification File                                                                     | String           |                      |                      |
| `targetFolder`  | Target folder for generated output                                                         | String           |                      |                      |
| `style`         | Programming style                                                                          | ProgrammingStyle | IMPERATIVE           | IMPERATIVE, REACTIVE |
| `exposeMessage` | Whether to expose underlying spring Message to consumers or not. Default: false            | boolean          | false                |                      |
| `apiPackage`    | Java API package name                                                                      | String           | io.example.api       |                      |
| `modelPackage`  | Java Models package name                                                                   | String           | io.example.api.model |                      |
| `bindingTypes`  | Binding names to include in code generation. Generates code for ALL bindings if left empty | List             |                      |                      |
| `role`          | Project role: PROVIDER\|CLIENT                                                             | RoleType         | PROVIDER             | PROVIDER, CLIENT     |
| `operationIds`  | Operation ids to include in code generation. Generates code for ALL if left empty          | List             | []                   |                      |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringCloudStream3Configuration --help
```

### JDLBackendApplicationDefaultConfiguration

Generates a full backend application using a flexible hexagonal architecture

This is the long description

**Options:**

| **Option**          | **Description**                                                            | **Type**         | **Default**             | **Values**           |
| ------------------- | -------------------------------------------------------------------------- | ---------------- | ----------------------- | -------------------- |
| `specFile`          | OpenAPI file to parse                                                      | String           |                         |                      |
| `targetFolder`      | Target folder to generate code to. If left empty, it will print to stdout. | File             |                         |                      |
| `specFiles`         | JDL files to parse                                                         | String[]         | [null]                  |                      |
| `entities`          | Entities to generate code for                                              | List             | []                      |                      |
| `persistence`       | Persistence                                                                | PersistenceType  | mongodb                 | mongodb              |
| `style`             | Programming Style                                                          | ProgrammingStyle | imperative              | imperative, reactive |
| `inputDTOSuffix`    | Suffix for CRUD operations DTOs (default: Input)                           | String           | Input                   |                      |
| `criteriaDTOSuffix` | Suffix for search criteria DTOs (default: Criteria)                        | String           | Criteria                |                      |
| `searchDTOSuffix`   | Suffix for elasticsearch document entities (default: Document)             | String           | Document                |                      |
| `basePackage`       | Java Models package name                                                   | String           | io.example.domain.model |                      |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration --help
```

### Java 2 JDL Reverse Engineering

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

### JDLOpenAPIControllersConfiguration

Generates implementations based on JDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.

${javadoc}

**Options:**

| **Option**                           | **Description**                                                                                                                                                            | **Type**         | **Default**                                      | **Values**           |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | ------------------------------------------------ | -------------------- |
| `jdlFile`                            | JDL file to parse                                                                                                                                                          | String           |                                                  |                      |
| `specFile`                           | API Specification File                                                                                                                                                     | String           |                                                  |                      |
| `targetFolder`                       | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                 | File             |                                                  |                      |
| `specFiles`                          | JDL files to parse                                                                                                                                                         | String[]         | [null]                                           |                      |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                      | String           | x-business-entity                                |                      |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)                                        | String           | x-business-entity-paginated                      |                      |
| `paginatedDtoItemsJsonPath`          | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List             | [$.properties.items, $.properties.content.items] |                      |
| `dtoToEntityNameMap`                 | Maps openapi dtos to jdl entity names                                                                                                                                      | Map              | {}                                               |                      |
| `controllersPackage`                 | The package to generate REST Controllers                                                                                                                                   | String           | {{basePackage}}.adapters.web                     |                      |
| `entitiesPackage`                    | Package where your domain entities are                                                                                                                                     | String           | {{basePackage}}.core.domain                      |                      |
| `inboundDtosPackage`                 | Package where your inbound dtos are                                                                                                                                        | String           | {{basePackage}}.core.inbound.dtos                |                      |
| `servicesPackage`                    | Package where your domain services/usecases interfaces are                                                                                                                 | String           | {{basePackage}}.core.inbound                     |                      |
| `inputDTOSuffix`                     | Suffix for CRUD operations DTOs (default: Input)                                                                                                                           | String           | Input                                            |                      |
| `entityDTOSuffix`                    | Suffix for (output) entities DTOs (default: empty to use the entity itself)                                                                                                | String           |                                                  |                      |
| `criteriaDTOSuffix`                  | Suffix for search criteria DTOs (default: Criteria)                                                                                                                        | String           | Criteria                                         |                      |
| `searchDTOSuffix`                    | Suffix for elasticsearch document entities (default: Document)                                                                                                             | String           | Document                                         |                      |
| `style`                              | Programming Style                                                                                                                                                          | ProgrammingStyle | imperative                                       | imperative, reactive |
| `basePackage`                        | Applications base package                                                                                                                                                  | String           |                                                  |                      |
| `openApiApiPackage`                  | The package to used by OpenAPI-Generator for generated api objects/classes                                                                                                 | String           |                                                  |                      |
| `openApiModelPackage`                | The package to used by OpenAPI-Generator for generated model objects/classes                                                                                               | String           | {{openApiApiPackage}}                            |                      |
| `openApiModelNamePrefix`             | Sets the prefix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                                  |                      |
| `openApiModelNameSuffix`             | Sets the suffix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                                  |                      |
| `operationIds`                       | OpenAPI operationIds to generate code for                                                                                                                                  | List             | []                                               |                      |
| `statusCodes`                        | Status codes to generate code for (default: 200, 201, 202 and 400                                                                                                          | List             | [200, 201, 202, 400]                             |                      |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLOpenAPIControllersConfiguration --help
```

### JDLToOpenAPIConfiguration

Generates a full OpenAPI definitions for CRUD operations from JDL models

${javadoc}

**Options:**

| **Option**                           | **Description**                                                                                                                                                            | **Type** | **Default**                           | **Values** |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------------------------------------- | ---------- |
| `specFile`                           | OpenAPI file to parse                                                                                                                                                      | String   |                                       |            |
| `targetFolder`                       | Target folder for generated output                                                                                                                                         | String   |                                       |            |
| `specFiles`                          | JDL files to parse                                                                                                                                                         | String[] | [null]                                |            |
| `entities`                           | Entities to generate code for                                                                                                                                              | List     | []                                    |            |
| `targetFile`                         | Target file                                                                                                                                                                | String   | openapi.yml                           |            |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                      | String   | x-business-entity                     |            |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)                                        | String   | x-business-entity-paginated           |            |
| `paginatedDtoItemsJsonPath`          | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List     | [$.items, $.properties.content.items] |            |
| `basePackage`                        | Java Models package name                                                                                                                                                   | String   | io.example.domain.model               |            |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration --help
```

### OpenAPIToJDLConfiguration

Generates JDL model from OpenAPI schemas

${javadoc}

**Options:**

| **Option**         | **Description**                                 | **Type** | **Default**             | **Values** |
| ------------------ | ----------------------------------------------- | -------- | ----------------------- | ---------- |
| `specFile`         | API Specification File                          | String   |                         |            |
| `targetFolder`     | Target folder for generated output              | String   |                         |            |
| `entities`         | Entities to generate code for                   | List     | []                      |            |
| `targetFile`       | Target file                                     | String   | entities.jdl            |            |
| `useRelationships` | Whether to use JDL relationships or plain field | boolean  | true                    |            |
| `basePackage`      | Java Models package name                        | String   | io.example.domain.model |            |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.OpenAPIToJDLConfiguration --help
```

### SpringWebTestClientConfiguration

Generates spring WebTestClient tests from OpenAPI defined endpoints.

${javadoc}

**Options:**

| **Option**               | **Description**                                                              | **Type**    | **Default**                  | **Values**                  |
| ------------------------ | ---------------------------------------------------------------------------- | ----------- | ---------------------------- | --------------------------- |
| `specFile`               | API Specification File                                                       | String      |                              |                             |
| `targetFolder`           | Target folder to generate code to. If left empty, it will print to stdout.   | File        |                              |                             |
| `controllersPackage`     | The package to generate REST Controllers                                     | String      | {{basePackage}}.adapters.web |                             |
| `groupBy`                | Generate test classes grouped by                                             | GroupByType | SERVICE                      | SERVICE, OPERATION, PARTIAL |
| `testSuffix`             | Class name suffix for generated test classes                                 | String      | IT                           |                             |
| `basePackage`            | Applications base package                                                    | String      |                              |                             |
| `openApiApiPackage`      | The package to used by OpenAPI-Generator for generated api objects/classes   | String      |                              |                             |
| `openApiModelPackage`    | The package to used by OpenAPI-Generator for generated model objects/classes | String      | {{openApiApiPackage}}        |                             |
| `openApiModelNamePrefix` | Sets the prefix for model enums and classes used by OpenAPI-Generator        | String      |                              |                             |
| `openApiModelNameSuffix` | Sets the suffix for model enums and classes used by OpenAPI-Generator        | String      |                              |                             |
| `operationIds`           | OpenAPI operationIds to generate code for                                    | List        | []                           |                             |
| `statusCodes`            | Status codes to generate code for (default: 200, 201, 202 and 400            | List        | [200, 201, 202, 400]         |                             |

**Getting Help:**

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringWebTestClientConfiguration --help
```
