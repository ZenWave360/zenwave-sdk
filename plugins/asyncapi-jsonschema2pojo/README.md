# AsyncAPI and JsonSchema2Pojo (with maven plugin)

Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files

${javadoc}

## Options

| **Option**        | **Description**                                                                                                | **Type** | **Default**          | **Values**       |
| ----------------- | -------------------------------------------------------------------------------------------------------------- | -------- | -------------------- | ---------------- |
| `specFile`        | API Specification File                                                                                         | String   |                      |                  |
| `targetFolder`    | Target folder to generate code to. If left empty, it will print to stdout.                                     | File     |                      |                  |
| `messageNames`    | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty | List     | []                   |                  |
| `jsonschema2pojo` | JsonSchema2Pojo settings                                                                                       | Map      | {}                   |                  |
| `apiPackage`      | Java API package name                                                                                          | String   | io.example.api       |                  |
| `modelPackage`    | Java Models package name                                                                                       | String   | io.example.api.model |                  |
| `bindingTypes`    | Binding names to include in code generation. Generates code for ALL bindings if left empty                     | List     |                      |                  |
| `role`            | Project role: provider\|client                                                                                 | RoleType | provider             | provider, client |
| `operationIds`    | Operation ids to include in code generation. Generates code for ALL if left empty                              | List     | []                   |                  |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoConfiguration --help
```

## Maven Plugin Configuration (API-First)

You can use ZenWave Maven Plugin to generate code as part of your build process:

- Adding this generator jar as dependency to zenwave maven plugin.
- Passing any configuration as <configOptions>.

```xml
<plugin>
    <groupId>io.github.zenwave360.zenwave-code-generator</groupId>
    <artifactId>zenwave-code-generator-mojo</artifactId>
    <version>${zenwave.version}</version>
    <executions>
        <execution>
            <id>generate-asyncapi-producer</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>spring-cloud-streams3</generatorName>
                <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
                <configOptions>
                    <role>provider</role>
                    <style>imperative</style>
                    <apiPackage>io.zenwave360.example.adapters.events.producer</apiPackage>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
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
