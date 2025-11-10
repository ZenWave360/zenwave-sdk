# Avro Schema Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)
Generates Java classes from Avro schemas using Avro Compiler.

Generates Java classes from Avro schemas using your provided Avro Compiler version. Compatible with Avro versions from 1.8.0 to 1.12.0.


## Usage

### Using the ZenWave CLI

Pointing to remote http files:

```shell
jbang zw -p io.zenwave360.sdk.plugins.AvroSchemaGeneratorPlugin \
    avroFiles="https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/Address.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethod.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethodType.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/CustomerEvent.avsc" \
    sourceFolder=src/main/java \
    targetFolder=target/generated-sources/avro
```

Pointing to local files:

```shell
jbang zw -p io.zenwave360.sdk.plugins.AvroSchemaGeneratorPlugin \
    avroCompilerProperties.sourceDirectory=some/avro/folder \
    avroCompilerProperties.imports="common.avsc,another.avsc" \
    sourceFolder=src/main/java \
    targetFolder=target/generated-sources/avro
```

### Using the ZenWave Maven Plugin

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
            <id>generate-avros</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>AvroSchemaGeneratorPlugin</generatorName>
                <configOptions>
                    <avroFiles>
                        https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/Address.avsc,
                        https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethod.avsc,
                        https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethodType.avsc,
                        https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/CustomerEvent.avsc
                    </avroFiles>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>avro-schema-compiler</artifactId>
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

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `avroFiles` | List of avro schema files to generate code for. It is alternative to sourceDirectory and imports. | List |  |   |
| `avroCompilerProperties` | Avro Compiler Properties | AvroCompilerProperties | See [AvroCompilerProperties](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/avro-schema-compiler/src/main/java/io/zenwave360/sdk/plugins/AvroCompilerProperties.java) |   |
| `avroCompilerProperties.sourceDirectory` | Avro schema file or folder containing avro schemas | File |  |   |
| `avroCompilerProperties.imports` | Avro schema files or folders containing avro schemas. It supports local files/folders, `classpath:` files/folders or `https://` file resources. | List |  |   |
| `avroCompilerProperties.includes` | A set of Ant-like inclusion patterns used to select files from the source tree that are to be processed. By default, the pattern **\/*.avsc is used to include all avro schema files. | List | [**/*.avsc] |   |
| `avroCompilerProperties.excludes` | A set of Ant-like exclusion patterns used to prevent certain files from being processed. By default, this set is empty such that no files are excluded. | List |  |   |
| `avroCompilerProperties.customLogicalTypeFactories` | Custom Logical Type Factories | List |  |   |
| `avroCompilerProperties.customConversions` | Custom Conversions | List |  |   |
| `sourceFolder` | Source folder inside folder to generate code to. | String |  |   |
| `targetFolder` | Target folder to generate code to. | File | target\generated-sources\avro |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.AvroSchemaGeneratorPlugin --help
```
