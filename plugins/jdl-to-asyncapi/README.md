> :point_right: ZenWave360 Helps You Create Software Easy to Understand

# JDL To AsyncAPI Generator

Generates a full AsyncAPI definitions for CRUD operations from JDL models

## Options

| **Option**                  | **Description**                                                                                       | **Type**     | **Default**             | **Values**   |
|-----------------------------|-------------------------------------------------------------------------------------------------------|--------------|-------------------------|--------------|
| `specFile`                  | Spec file to parse                                                                                    | String       |                         |              |
| `targetFolder`              | Target folder for generated output                                                                    | String       |                         |              |
| `specFiles`                 | JDL files to parse                                                                                    | String[]     | []                      |              |
| `entities`                  | Entities to generate code for                                                                         | List         | []                      |              |
| `annotations`               | Annotations to generate code for (ex. aggregate)                                                      | List         | []                      |              |
| `skipForAnnotations`        | Skip generating operations for entities annotated with these                                          | List         | [vo, embedded, skip]    |              |
| `includeEvents`             | Include channels and messages to publish domain events                                                | boolean      | true                    |              |
| `includeCommands`           | Include channels and messages to listen for async command requests                                    | boolean      | false                   |              |
| `targetFile`                | Target file                                                                                           | String       | asyncapi.yml            |              |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) | String       | x-business-entity       |              |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat | schema                  | schema, avro |
| `avroPackage`               | Package name for generated Avro Schemas (.avsc)                                                       | String       | io.example.domain.model |              |
| `basePackage`               | Java Models package name                                                                              | String       | io.example.domain.model |              |
| `skipFormatting`            | Skip java sources output formatting                                                                   | boolean      | false                   |              |
| `haltOnFailFormatting`      | Halt on formatting errors                                                                             | boolean      | true                    |              |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIPlugin --help
```
