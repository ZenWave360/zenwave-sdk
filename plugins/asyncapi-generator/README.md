# AsyncAPI Generator for Java / Spring-Boot
> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

![AsyncAPI and Spring Cloud Streams 3](../../docs/ZenWave360-AsyncAPI-SpringCloudStreams.excalidraw.svg)

The ZenWave AsyncAPI Generator solves a long-standing issue in event-driven Java applications: keeping message models and channel contracts fully aligned with their AsyncAPI specification. It provides build-time read-only code generation from AsyncAPI files sourced from their canonical locations: local files, classpath resources, or authenticated remote URLs.

This approach eliminates API drift by enforcing the AsyncAPI file as the single source of truth for message schemas, channel definitions, and producer or consumer interfaces.

All generated classes are strongly typed and annotated for validation, and any breaking change in the specification results in a compile-time error.

This model aligns with the proven pattern used by the OpenAPI Maven Generator plugin, which solved API drift for OpenAPI in the Java ecosystem, generating non-editable interfaces and DTOs, and ensures contract consistency across consumers and producers.

You just need to focus on implementing your business logic, and configuring either Spring Cloud Streams or Spring Kafka for message transport.

A complete working example is available in the zenwave-playground repository: [asyncapi-shopping-cart](https://github.com/ZenWave360/zenwave-playground/blob/main/examples/asyncapi-shopping-cart/README.md).


<!-- TOC -->
* [AsyncAPI Generator for Java / Spring-Boot](#asyncapi-generator-for-java--spring-boot)
    * [Features](#features)
    * [Quick Configuration](#quick-configuration)
    * [Command Line Usage](#command-line-usage)
    * [Maven Usage](#maven-usage)
        * [Properties Configuration](#properties-configuration)
        * [Plugin Configuration](#plugin-configuration)
    * [Gradle Usage](#gradle-usage)
    * [Configuration Options](#configuration-options)
    * [Getting Help](#getting-help)
<!-- TOC -->

## Features

### Generated Code
- Complete Producer and Consumer implementations for Java and Spring Boot with thin wrappers around:
    - **Spring Cloud Stream** for multi-broker support
    - **Spring Kafka** for native Kafka integrations
- **JSON DTOs** generated via [jsonschema2pojo](https://www.jsonschema2pojo.org/) library
- **Avro DTOs** generated via [Apache Avro](https://avro.apache.org/docs/current/gettingstartedjava/) library

### Supported Capabilities
- AsyncAPI v2 and v3
- Local files, classpath files, and authenticated remote URLs
- DTO generation for JSON Schema and Avro
- Automatic Avro schema ordering for Avro versions prior to 1.12.0
- Strongly typed header objects from AsyncAPI message definitions

### Advanced Patterns
- Transactional Outbox pattern using Spring Modulith
- Role reversal to generate only the side you need (provider or client)
- Operation-level filtering for selective interface generation
- Automatic header mapping at runtime through the `x-runtime-expression` extension

## Quick Configuration

Use these essential configuration options to get the generator running quickly:

- **inputSpec**: Path or URL for the AsyncAPI specification
- **role**: `provider` (default) or `client`
- **templates**: `SpringCloudStream` (default) or `SpringKafka`
- **modelPackage**: Java package for generated DTOs (required for JSON Schema)
- **producerApiPackage**: Java package for producer interfaces (provider role)
- **consumerApiPackage**: Java package for consumer interfaces (client role)
- **avroCompilerProperties.imports**: Comma-separated Avro files or folders
- **operationIds**: Filter to generate only selected operations
- **authentication**: Credentials for authenticated remote AsyncAPI files

Advanced customization is available through:

- **avroCompilerProperties.xxx**: Settings passed to the underlying Avro compiler
- **jsonschema2pojo.xxx**: Settings passed to jsonschema2pojo

## Why ZenWave doesn’t source Spring Cloud Stream / Spring Kafka config from AsyncAPI

ZenWaveSDK provides all the building blocks to prevent API drift, keeping your source code aligned with your AsyncAPI specification. But it does not attempt to auto-configure Spring Cloud Stream or Spring Kafka from the AsyncAPI file.

While configuring Spring Cloud Stream or Spring Kafka from AsyncAPI would be convenient, it is not currently feasible.

AsyncAPI, even with specific bindings like Kafka bindings, does not yet provide enough information to fully configure Spring Kafka or Spring Cloud Stream:

- no standard fields for `acks`, `transaction.id`, `idempotence`, and similar settings
- Avro SerDes configuration is too ambiguous to infer automatically
- Kafka bindings define `clientId` and `consumerGroupId` per operation, although these are typically application-wide
- you can configure a schema registry endpoint but not compatibility modes (BACKWARD, FORWARD, etc.) for subjects or topics

This is why ZenWave does not attempt full auto-configuration from AsyncAPI. The specification is not expressive enough for these frameworks.

Still, this is a one-time operation that belongs in your Spring configuration files, where it can be controlled explicitly, versioned with your application, and tuned independently from your AsyncAPI contract.

## Command Line Usage

```shell
jbang zw -p AsyncAPIGeneratorPlugin \
  authentication[0].key=API_KEY \
  authentication[0].value=API_KEY_VALUE \
  apiFile=https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/asyncapi.yml \
  consumerApiPackage=io.example.api.consumer \
  producerApiPackage=io.example.api.producer \
  modelPackage=io.example.api.model \
  avroCompilerProperties.imports="\
    https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/Item.avsc,\
    https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/ShoppingCart.avsc" \
  targetFolder=target/generated-sources
```

## Maven Usage

### Properties Configuration

```xml
<properties>
    <asyncapiPrefix>classpath:io/example/asyncapi/shoppingcart/apis</asyncapiPrefix>
    <asyncapi.inputSpec>${asyncapiPrefix}/asyncapi.yml</asyncapi.inputSpec>
    <asyncapi.avro.imports>
        ${asyncapiPrefix}/avro/
    </asyncapi.avro.imports>

    <zenwave.asyncapiGenerator.templates>SpringKafka</zenwave.asyncapiGenerator.templates>

    <asyncApiProducerApiPackage>${basePackage}.events</asyncApiProducerApiPackage>
    <asyncApiConsumerApiPackage>${basePackage}.commands</asyncApiConsumerApiPackage>
</properties>
```

### Plugin Configuration

```xml
<plugin>
    <groupId>io.zenwave360.sdk</groupId>
    <artifactId>zenwave-sdk-maven-plugin</artifactId>
    <version>${zenwave.version}</version>
    <configuration>
        <inputSpec>${asyncapi.inputSpec}</inputSpec>
        <skip>false</skip>
        <addCompileSourceRoot>true</addCompileSourceRoot>
        <addTestCompileSourceRoot>true</addTestCompileSourceRoot>
        <authentication>
            <authentication>
                <key>API_KEY</key>
                <value>XXXXXX</value>
            </authentication>
        </authentication>
    </configuration>
    <executions>
        <execution>
            <id>generate-asyncapi</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>AsyncAPIGenerator</generatorName>
                <configOptions>
                    <role>provider</role>
                    <templates>${zenwave.asyncapiGenerator.templates}</templates>
                    <modelPackage>${asyncApiModelPackage}</modelPackage> <!--  required only for json-schema, otherwise it uses avro package -->
                    <producerApiPackage>${asyncApiProducerApiPackage}</producerApiPackage>
                    <consumerApiPackage>${asyncApiConsumerApiPackage}</consumerApiPackage>
                    <avroCompilerProperties.imports>${asyncapi.avro.imports}</avroCompilerProperties.imports><!-- comma separated list of avro files or folders -->
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-generator</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro-compiler</artifactId>
            <version>${avro.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-databind</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
</plugin>
```

## Gradle Usage

```kotlin
val asyncapiPrefix = "classpath:io/example/asyncapi/shoppingcart/apis"
val asyncapiInputSpec = "$asyncapiPrefix/asyncapi.yml"
val asyncapiAvroImports = "$asyncapiPrefix/avro/"
val asyncapiGeneratorTemplates = "SpringKafka"
val asyncApiProducerApiPackage = "io.zenwave360.examples.events"
val asyncApiConsumerApiPackage = "io.zenwave360.examples.commands"

plugins {
    java
    id("dev.jbang") version "0.3.0"
}

tasks.register<dev.jbang.gradle.tasks.JBangTask>("generateAsyncApiProvider") {
    group = "asyncapi"
    description = "Generates Producer and Consumer code from AsyncAPI specification"
    script.set("io.zenwave360.sdk:zenwave-sdk-cli:RELEASE")
    jbangArgs.set(listOf(
        "--deps=" +
            "org.slf4j:slf4j-simple:1.7.36," +
            "io.zenwave360.sdk.plugins:asyncapi-generator:RELEASE," +
            "org.apache.avro:avro-compiler:1.11.1",
        "--java-options \"--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED\"",
        "--java-options \"--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED\"",
        "--java-options \"--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED\"",
        "--java-options \"--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED\"",
        "--java-options \"--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED\""
    ))
    args.set(listOf(
        "-p", "AsyncAPIGenerator",
        "role=provider",
        "templates=$asyncapiGeneratorTemplates",
        "apiFile=$asyncapiInputSpec",
        "targetFolder=${layout.buildDirectory.dir("generated-sources/zenwave").get().asFile.absolutePath}",
        "transactionalOutbox=modulith",
        "modelPackage=$asyncApiModelPackage",
        "producerApiPackage=$asyncApiProducerApiPackage",
        "consumerApiPackage=$asyncApiConsumerApiPackage",
        "avroCompilerProperties.imports=$asyncapiAvroImports"
    ))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/zenwave/src/main/java").get().asFile)
        }
    }
    test {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/zenwave/src/test/java").get().asFile)
        }
    }
}
```

## Configuration Options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-----------|------------|
| `apiFile` | API Specification File | URI |  |   |
| `role` | Project role: provider/client | AsyncapiRoleType | provider | provider, client  |
| `templates` | Templates to use for code generation. | String | SpringCloudStream | SpringCloudStream, SpringKafka, FQ Class Name  |
| `modelPackage` | Java Models package name | String |  |   |
| `producerApiPackage` | Java API package name for outbound (producer) services. It can override apiPackage for producers. | String | {{apiPackage}} |   |
| `consumerApiPackage` | Java API package name for inbound (consumer) services. It can override apiPackage for consumer. | String | {{apiPackage}} |   |
| `apiPackage` | Java API package, if `producerApiPackage` and `consumerApiPackage` are not set. | String |  |   |
| `operationIds` | Operation ids to include in code generation. Generates code for ALL if left empty | List | [] |   |
| `excludeOperationIds` | Operation ids to exclude in code generation. Skips code generation if is not included or is excluded. | List | [] |   |
| `transactionalOutbox` | Transactional outbox type for message producers. | TransactionalOutboxType | none | none, modulith  |
| `jsonschema2pojo` | JsonSchema2Pojo settings for downstream library [(docs)](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-jsonschema2pojo/src/main/java/io/zenwave360/sdk/plugins/JsonSchema2PojoConfiguration.java) | Map | {} |   |
| `avroCompilerProperties` | Avro Compiler Properties | AvroCompilerProperties | See [AvroCompilerProperties](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/avro-schema-compiler/src/main/java/io/zenwave360/sdk/plugins/AvroCompilerProperties.java) |   |
| `avroCompilerProperties.sourceDirectory` | Avro schema file or folder containing avro schemas | File |  |   |
| `avroCompilerProperties.imports` | Avro schema files or folders containing avro schemas. It supports local files/folders, `classpath:` files/folders or `https://` file resources. | List |  |   |
| `avroCompilerProperties.includes` | A set of Ant-like inclusion patterns used to select files from the source tree that are to be processed. By default, the pattern **\/*.avsc is used to include all avro schema files. | List | [**/*.avsc] |   |
| `avroCompilerProperties.excludes` | A set of Ant-like exclusion patterns used to prevent certain files from being processed. By default, this set is empty such that no files are excluded. | List |  |   |
| `avroCompilerProperties.customLogicalTypeFactories` | Custom Logical Type Factories | List |  |   |
| `avroCompilerProperties.customConversions` | Custom Conversions | List |  |   |
| `componentPrefix` | Prefix used in to reference this component in @Component and application.yml | String |  |   |
| `componentSuffix` | Suffix used in to reference this component in @Component and application.yml | String |  |   |
| `generatedAnnotationClass` | Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports. | String |  |   |
| `authentication` | Authentication configuration values for fetching remote resources. | List | [] |   |
| `targetFolder` | Target folder to generate code to. If left empty, it will print to stdout. | File |  |   |
| `modelNamePrefix` | Sets the prefix for model classes and enums | String |  |   |
| `modelNameSuffix` | Sets the suffix for model classes and enums | String |  |   |
| `runtimeHeadersProperty` | AsyncAPI extension property name for runtime auto-configuration of headers. | String | x-runtime-expression |   |
| `messageNames` | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty | List | [] |   |
| `sourceFolder` | Source folder inside folder to generate code to. | String | src/main/java |   |
| `bindingTypes` | Binding names to include in code generation. Generates code for ALL bindings if left empty | List |  |   |
| `avroFiles` | List of avro schema files to generate code for. It is alternative to sourceDirectory and imports. | List |  |   |
| `skipProducerImplementation` | Generate only the producer interface and skip the implementation. | boolean | false |   |
| `exposeMessage` | Whether to expose underlying spring Message to consumers or not. | boolean | false |   |
| `useEnterpriseEnvelope` | Include support for enterprise envelop wrapping/unwrapping. | boolean | false |   |
| `envelopeJavaTypeExtensionName` | AsyncAPI Message extension name for the envelop java type for wrapping/unwrapping. | String | x-envelope-java-type |   |
| `methodAndMessageSeparator` | To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces. | String | $ |   |
| `consumerPrefix` | Consumer object class name prefix | String |  |   |
| `consumerSuffix` | Consumer object class name suffix | String | Consumer |   |
| `consumerServicePrefix` | Business/Service interface prefix | String | I |   |
| `consumerServiceSuffix` | Business/Service interface suffix | String | ConsumerService |   |
| `formatter` | Code formatter implementation | Formatters | palantir | palantir, spring, google  |
| `skipFormatting` | Skip java sources output formatting | boolean | false |   |
| `haltOnFailFormatting` | Halt on formatting errors | boolean | true |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.AsyncAPIGeneratorPlugin --help
```
