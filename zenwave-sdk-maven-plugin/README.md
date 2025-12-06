# ZenWave SDK Maven and Gradle Support

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

## ZenWave SDK Maven Plugin

You can use ZenWave Maven Plugin to generate code as part of your build process:

- Add each generator jar as dependency to zenwave maven plugin.
- Pass any generator plugin as `<configOptions>`.

In the following example we are configuring the `asyncapi-spring-cloud-streams3` and `asyncapi-jsonschema2pojo` generators to generate Spring Cloud Streams 3.x code and DTOs from an AsyncAPI definition:

> **Note**: Starting with version 2.0.0, the Maven `groupId` has changed to `io.zenwave360`. The code remains fully compatible.

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
            <plugin>
                <generatorName>spring-cloud-streams3</generatorName>
                <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
                <configOptions>
                    <role>provider</role>
                    <style>imperative</style>
                    <apiPackage>io.zenwave360.example.adapters.events.producer</apiPackage>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
                </configOptions>
            </plugin>
        </execution>
        <execution>
            <id>generate-asyncapi-producer-dtos</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <plugin>
                <generatorName>jsonschema2pojo</generatorName>
                <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
                <configOptions>
                    <apiPackage>io.zenwave360.example.adapters.events</apiPackage>
                    <modelPackage>io.zenwave360.example.adapters.events.model</modelPackage>
                </configOptions>
            </plugin>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-spring-cloud-streams3</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-jsonschema2pojo</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

## Gradle Usage

ZenWave SDK can be used as a [JBang](https://www.jbang.dev) task in your Gradle build.

Use:

- `dev.jbang:jbang-gradle-plugin:0.3.0` to run jbang tasks
- `io.zenwave360.sdk:zenwave-sdk-cli:RELEASE` as jbang script
- any plugin jar as dependency, including custom ones
- add `args` to pass any plugin option

This is an example of how to configure `io.zenwave360.sdk.plugins:asyncapi-generator`

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
            "org.apache.avro:avro-compiler:1.11.1"
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
