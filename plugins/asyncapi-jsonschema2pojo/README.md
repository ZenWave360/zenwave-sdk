# AsyncAPI and JsonSchema2Pojo (with maven plugin)
> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files using https://www.jsonschema2pojo.org/ library.

```shell
jbang zw -p io.zenwave360.sdk.plugins.AsyncApiJsonSchema2PojoPlugin \
    apiFile=src/main/resources/model/asyncapi.yml \
    modelPackage=io.zenwave360.example.core.domain.events \
    jsonschema2pojo.includeTypeInfo=true \
    targetFolder=.
```

## Options

| **Option**                 | **Description**                                                                                                                                                       | **Type**                    | **Default** | **Values** |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|-------------|------------|
| `apiFile`                  | API Specification File                                                                                                                                                | URI                         |             |            |
| `apiFiles`                 | API Spec files to parse (comma separated)                                                                                                                             | List                        |             |            |
| `targetFolder`             | Target folder to generate code to.                                                                                                                                    | File                        |             |            |
| `modelPackage`             | Java Models package name                                                                                                                                              | String                      |             |            |
| `generatedAnnotationClass` | Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports. | Class<? extends Annotation> |             |            |
| `jsonschema2pojo`          | JsonSchema2Pojo settings                                                                                                                                              | Map                         | {}          |            |
| `modelNamePrefix`          | Sets the prefix for model classes and enums                                                                                                                           | String                      |             |            |
| `modelNameSuffix`          | Sets the suffix for model classes and enums                                                                                                                           | String                      |             |            |
| `messageNames`             | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty                                                        | List                        | []          |            |
| `bindingTypes`             | Binding names to include in code generation. Generates code for ALL bindings if left empty                                                                            | List                        |             |            |
| `operationIds`             | Operation ids to include in code generation. Generates code for ALL if left empty                                                                                     | List                        | []          |            |
| `excludeOperationIds`      | Operation ids to exclude in code generation. Skips code generation if is not included or is excluded.                                                                 | List                        | []          |            |


## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.AsyncApiJsonSchema2PojoPlugin --help
```

## Maven Plugin Configuration (API-First)

You can use ZenWave Maven Plugin to generate code as part of your build process:

- Adding this generator jar as dependency to zenwave maven plugin.
- Passing any configuration as <configOptions>.

Use jsonschema2pojo prefix to pass any option to https://www.jsonschema2pojo.org/ underlying library.

```xml
<plugin>
    <groupId>io.zenwave360.sdk</groupId>
    <artifactId>zenwave-sdk-maven-plugin</artifactId>
    <version>${zenwave.version}</version>
    <executions>
        <execution>
            <id>generate-asyncapi-producer</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>jsonschema2pojo</generatorName>
                <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
                <configOptions>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
                    <!-- use jsonschema2pojo prefix to pass any option to jsonschema2pojo underlying library -->
                    <jsonschema2pojo.includeTypeInfo>true</jsonschema2pojo.includeTypeInfo>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-jsonschema2pojo</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```
