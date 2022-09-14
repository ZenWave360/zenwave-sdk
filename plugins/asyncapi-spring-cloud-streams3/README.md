# SpringCloudStream3Configuration

Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI

${javadoc}

## Options

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

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringCloudStream3Configuration --help
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
                    <role>PROVIDER</role>
                    <style>IMPERATIVE</style>
                    <apiPackage>io.zenwave360.example.adapters.events.producer</apiPackage>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
                </configOptions>
            </configuration>
        </execution>
        <execution>
            <id>generate-asyncapi-producer-dtos</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>jsonschema2pojo</generatorName>
                <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
                <configOptions>
                    <apiPackage>io.zenwave360.example.adapters.events</apiPackage>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.github.zenwave360.zenwave-code-generator.plugins</groupId>
            <artifactId>asyncapi-spring-cloud-streams3</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.zenwave360.zenwave-code-generator.plugins</groupId>
            <artifactId>asyncapi-jsonschema2pojo</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```