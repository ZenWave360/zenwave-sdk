# AsyncAPI and JsonSchema2Pojo (with maven plugin)
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zenwave360.zenwave-code-generator/zenwave-code-generator.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.github.zenwave360.zenwave-code-generator/zenwave-code-generator)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-code-generator)](https://github.com/ZenWave360/zenwave-code-generator/blob/main/LICENSE)

Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files using https://www.jsonschema2pojo.org/ library.

## Options

| **Option**        | **Description**                                                                                                | **Type**         | **Default**   | **Values**       |
|-------------------|----------------------------------------------------------------------------------------------------------------|------------------|---------------|------------------|
| `specFile`        | API Specification File                                                                                         | String           |               |                  |
| `targetFolder`    | Target folder to generate code to.                                                                             | File             |               |                  |
| `messageNames`    | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty | List             | []            |                  |
| `jsonschema2pojo` | JsonSchema2Pojo settings to pass any option to https://www.jsonschema2pojo.org/ underlying library.            | Map              | {}            |                  |
| `sourceFolder`    | Source folder inside folder to generate code to.                                                               | String           | src/main/java |                  |
| `apiPackage`      | Java API package name                                                                                          | String           |               |                  |
| `modelPackage`    | Java Models package name                                                                                       | String           |               |                  |
| `bindingTypes`    | Binding names to include in code generation. Generates code for ALL bindings if left empty                     | List             |               |                  |
| `role`            | Project role: provider/client                                                                                  | AsyncapiRoleType | provider      | provider, client |
| `operationIds`    | Operation ids to include in code generation. Generates code for ALL if left empty                              | List             | []            |                  |



## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoPlugin --help
```

## Maven Plugin Configuration (API-First)

You can use ZenWave Maven Plugin to generate code as part of your build process:

- Adding this generator jar as dependency to zenwave maven plugin.
- Passing any configuration as <configOptions>.

Use jsonschema2pojo prefix to pass any option to https://www.jsonschema2pojo.org/ underlying library.

```xml
<plugin>
    <groupId>io.github.zenwave360.zenwave-code-generator</groupId>
    <artifactId>zenwave-code-generator-maven-plugin</artifactId>
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
            <groupId>io.github.zenwave360.zenwave-code-generator.plugins</groupId>
            <artifactId>asyncapi-jsonschema2pojo</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```
