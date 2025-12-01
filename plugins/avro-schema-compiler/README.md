# Avro Schema Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

The Avro Schema Generator produces Java classes from Avro schemas using your chosen Avro Compiler version.  
It supports Avro versions from 1.8.0 up to 1.12.0.

### Why use this Avro Compiler Plugin?

- Sources Avro schemas from **local files**, **classpath resources**, or **authenticated remote HTTP URLs**.
- Automatically **sorts schemas** to resolve dependencies for Avro versions prior to 1.12.0.
- Ensures generated code is consistent and ready for integration in Java projects.

<!-- TOC -->
* [Avro Schema Generator](#avro-schema-generator)
    * [Why use this Avro Compiler Plugin?](#why-use-this-avro-compiler-plugin)
  * [Usage](#usage)
    * [Using the ZenWave CLI](#using-the-zenwave-cli)
    * [Using the ZenWave Maven Plugin](#using-the-zenwave-maven-plugin)
    * [Gradle Usage](#gradle-usage)
  * [Options](#options)
  * [Getting Help](#getting-help)
<!-- TOC -->

## Usage

### Using the ZenWave CLI

Generating from remote HTTP resources:

```shell
jbang zw -p io.zenwave360.sdk.plugins.AvroSchemaGeneratorPlugin \
    avroFiles="https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/Address.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethod.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethodType.avsc, \
              https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/CustomerEvent.avsc" \
    sourceFolder=src/main/java \
    targetFolder=target/generated-sources/avro
```

This will generate the following Java classes:

```shell
$ find target/ -type f
target/generated-sources/avro/src/main/java/io/zenwave360/example/core/outbound/events/dtos/Address.java
target/generated-sources/avro/src/main/java/io/zenwave360/example/core/outbound/events/dtos/CustomerEvent.java
target/generated-sources/avro/src/main/java/io/zenwave360/example/core/outbound/events/dtos/PaymentMethod.java
target/generated-sources/avro/src/main/java/io/zenwave360/example/core/outbound/events/dtos/PaymentMethodType.java
```

Generating from local folders and imports:

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
        <!-- <authentication> -->
        <!--     <authentication><key>API_KEY</key><value>XXX</value></authentication> -->
        <!-- </authentication> -->
        <addCompileSourceRoot>true</addCompileSourceRoot>!-- default is true -->
        <addTestCompileSourceRoot>true</addTestCompileSourceRoot>!-- default is false -->
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
            <version>${zenwave.version}</version> <!-- Requires 2.2.0 or newer -->
        </dependency>
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro-compiler</artifactId>
            <version>${avro-compiler.version}</version> <!-- Supports 1.8.0 to 1.12.0+ -->
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

NOTE: you need to exclude jackson-core and jackson-databind from avro-compiler dependency to avoid conflicts with ZenWaveSDK requirements which expect newer versions.

### Gradle Usage

```kotlin
plugins {
    java
    id("dev.jbang") version "0.3.0"
}

tasks.register<dev.jbang.gradle.tasks.JBangTask>("generateAvroClasses") {
    group = "avro"
    description = "Generates Avro classes from Avro schemas"
    script.set("io.zenwave360.sdk:zenwave-sdk-cli:RELEASE")
    jbangArgs.set(listOf(
        "--deps=" +
            "org.slf4j:slf4j-simple:1.7.36," +
            "io.zenwave360.sdk.plugins:avro-schema-compiler:RELEASE," +
            "org.apache.avro:avro-compiler:1.11.1"
    ))
    args.set(listOf(
        "-p", "AvroSchemaGeneratorPlugin",
        "avroFiles=" +
            "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/Address.avsc," +
            "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethod.avsc," +
            "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/PaymentMethodType.avsc," +
            "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/main/plugins/avro-schema-compiler/src/test/resources/avros/customer-event/CustomerEvent.avsc"
    ))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/zenwave/src/main/java").get().asFile)
        }
    }
}
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
