# AsyncAPI and Spring Cloud Stream with Avro and JSON
> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI with Avro and JSON DTOs

## Maven Usage

- Configure `zenwave-sdk-maven-plugin` in your `pom.xml`
- Add `asyncapi-spring-cloud-streams-with-avro-json` as dependency to `zenwave-sdk-maven-plugin`
- Bring your own version of `avro-compiler` as dependency to `zenwave-sdk-maven-plugin`, supporting versions 1.8.0 - 1.12.0+
- Include maven dependencies holding your API files in case you are using `classpath:` scheme in `inputSpec`.
- Set `<generatorName>SpringCloudStreamsWithDtosPlugin</generatorName>`
- Point to your AsyncAPI file in `inputSpec`: supported schemes are `classpath:`, `https://` and regular local files.
- Configure basic options:
  - `producerApiPackage`,`consumerApiPackage`: producer/consumer packages (for events/commands respectively)
  - `modelPackage`: package for generated DTOs in case of JSON payloads
  - `avroCompilerProperties.*`: options like `imports`, `includes`, `excludes`, `customLogicalTypeFactories`, `customConversions`, etc... See [AvroCompilerProperties](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/avro-schema-compiler/src/main/java/io/zenwave360/sdk/plugins/AvroCompilerProperties.java)
  - `jsonschema2pojo.*`: JsonSchema2Pojo settings for downstream library. See [JsonSchema2PojoConfiguration.java](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-jsonschema2pojo/src/main/java/io/zenwave360/sdk/plugins/JsonSchema2PojoConfiguration.java)

```xml
<plugin>
    <groupId>io.zenwave360.sdk</groupId>
    <artifactId>zenwave-sdk-maven-plugin</artifactId>
    <version>${zenwave.version}</version>
    <configuration>
        <addCompileSourceRoot>true</addCompileSourceRoot><!-- default is true -->
        <addTestCompileSourceRoot>true</addTestCompileSourceRoot><!-- default is true -->
    </configuration>
    <executions>
        <execution>
            <id>generate-asyncapi-with-dtos</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>SpringCloudStreamsWithDtosPlugin</generatorName>
                <inputSpec>classpath:model/asyncapi.yml</inputSpec><!-- supported schemes are `classpath:`, `https://` and regular local files -->
                <configOptions>
                    <producerApiPackage>io.example.api.producer</producerApiPackage>
                    <consumerApiPackage>io.example.api.consumer</consumerApiPackage>

                    <avroCompilerProperties.imports>classpath:avro</avroCompilerProperties.imports><!-- supports local files/folders, `classpath:` files/folders or `https://` file resources -->

                    <modelPackage>io.example.api.model</modelPackage>
                    <jsonschema2pojo.includeTypeInfo>true</jsonschema2pojo.includeTypeInfo>
                    
                    <transactionalOutbox>modulith</transactionalOutbox>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-spring-cloud-streams-with-avro-json</artifactId>
            <version>${zenwave.version}</version><!-- 2.2.0+ -->
        </dependency>
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro-compiler</artifactId>
            <version>${avro-compiler.version}</version><!-- 1.8.0 - 1.12.0+ -->
        </dependency>
    </dependencies>
</plugin>
```



## Options

| **Option**                                          | **Description** | **Type** | **Default** | **Values** |
|-----------------------------------------------------|-----------------|----------|-------------|------------|
| `apiFile`                                           | API Specification File  | URI |  |   |
| `role`                                              | Project role: provider/client  | AsyncapiRoleType | provider | provider, client  |
| `style`                                             | Programming style  | ProgrammingStyle | imperative | imperative, reactive  |
| `modelPackage`                                      | Java Models package name  | String |  |   |
| `producerApiPackage`                                | Java API package name for outbound (producer) services. It can override apiPackage for producers.  | String | {{apiPackage}} |   |
| `consumerApiPackage`                                | Java API package name for inbound (consumer) services. It can override apiPackage for consumer.  | String | {{apiPackage}} |   |
| `apiPackage`                                        | Java API package, if `producerApiPackage` and `consumerApiPackage` are not set.  | String |  |   |
| `jsonschema2pojo`                                   | JsonSchema2Pojo settings for downstream library [(docs)](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-jsonschema2pojo/src/main/java/io/zenwave360/sdk/plugins/JsonSchema2PojoConfiguration.java) | Map | {} |   |
| `avroCompilerProperties`                            | Avro Compiler Properties  | AvroCompilerProperties | See [AvroCompilerProperties](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/avro-schema-compiler/src/main/java/io/zenwave360/sdk/plugins/AvroCompilerProperties.java) |   |
| `avroCompilerProperties.sourceDirectory`            | Avro schema file or folder containing avro schemas  | File |  |   |
| `avroCompilerProperties.imports`                    | Avro schema files or folders containing avro schemas. It supports local files/folders, `classpath:` files/folders or `https://` file resources.  | List |  |   |
| `avroCompilerProperties.includes`                   | A set of Ant-like inclusion patterns used to select files from the source tree that are to be processed. By default, the pattern **\/*.avsc is used to include all avro schema files.  | List | [**/*.avsc] |   |
| `avroCompilerProperties.excludes`                   | A set of Ant-like exclusion patterns used to prevent certain files from being processed. By default, this set is empty such that no files are excluded.  | List |  |   |
| `avroCompilerProperties.customLogicalTypeFactories` | Custom Logical Type Factories  | List |  |   |
| `avroCompilerProperties.customConversions`          | Custom Conversions  | List |  |   |
| `operationIds`                                      | Operation ids to include in code generation. Generates code for ALL if left empty  | List | [] |   |
| `messageNames`                                      | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty  | List | [] |   |
| `excludeOperationIds`                               | Operation ids to exclude in code generation. Skips code generation if is not included or is excluded.  | List | [] |   |
| `bindingTypes`                                      | Binding names to include in code generation. Generates code for ALL bindings if left empty  | List |  |   |
| **JsonSchema2Pojo Options**                         | | | | |
| `modelNamePrefix`                                   | Sets the prefix for model classes and enums  | String |  |   |
| `modelNameSuffix`                                   | Sets the suffix for model classes and enums  | String |  |   |
| `generatedAnnotationClass`                          | Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports.  | String |  |   |
| **SpringCloudStreams3 Options**                     | | | | |
| `transactionalOutbox`                               | Transactional outbox type for message producers.  | TransactionalOutboxType | none | none, modulith, mongodb, jdbc  |
| `bindingPrefix`                                     | SC Streams Binding Name Prefix (used in @Component name)  | String |  |   |
| `bindingSuffix`                                     | Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. <operation-id>-in-0  | String | -0 |   |
| `runtimeHeadersProperty`                            | AsyncAPI extension property name for runtime auto-configuration of headers.  | String | x-runtime-expression |   |
| `includeApplicationEventListener`                   | Include ApplicationEvent listener for consuming messages within the modulith.  | boolean | false |   |
| `skipProducerImplementation`                        | Generate only the producer interface and skip the implementation.  | boolean | false |   |
| `exposeMessage`                                     | Whether to expose underlying spring Message to consumers or not.  | boolean | false |   |
| `useEnterpriseEnvelope`                             | Include support for enterprise envelop wrapping/unwrapping.  | boolean | false |   |
| `envelopeJavaTypeExtensionName`                     | AsyncAPI Message extension name for the envelop java type for wrapping/unwrapping.  | String | x-envelope-java-type |   |
| `methodAndMessageSeparator`                         | To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces.  | String | $ |   |
| `consumerPrefix`                                    | SC Streams Binder class prefix  | String |  |   |
| `consumerSuffix`                                    | SC Streams Binder class suffix  | String | Consumer |   |
| `consumerServicePrefix`                             | Business/Service interface prefix  | String | I |   |
| `consumerServiceSuffix`                             | Business/Service interface suffix  | String | ConsumerService |   |
| **General Options**                                 | | | | |
| `sourceFolder`                                      | Source folder inside folder to generate code to.  | String | src/main/java |   |
| `targetFolder`                                      | Target folder to generate code to. If left empty, it will print to stdout.  | File |  |   |
| `formatter`                                         | Code formatter implementation  | Formatters | palantir | palantir, spring, google  |
| `skipFormatting`                                    | Skip java sources output formatting  | boolean | false |   |
| `haltOnFailFormatting`                              | Halt on formatting errors  | boolean | true |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.AsyncAPIGeneratorPlugin``
