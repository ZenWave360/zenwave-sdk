# JDL To AsyncAPI Generator

Generates a full AsyncAPI definitions for CRUD operations from JDL models

${javadoc}

## Options


| **Option**                  | **Description**                                                                                       | **Type**     | **Default**             | **Values**  |
|-----------------------------|-------------------------------------------------------------------------------------------------------|--------------|-------------------------|-------------|
| `specFile`                  | Spec file to parse                                                                                    | String       |                         |             |
| `targetFolder`              | Target folder for generated output                                                                    | String       |                         |             |
| `specFiles`                 | JDL files to parse                                                                                    | String[]     | [null]                  |             |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat | schema                  | schema,avro |
| `entities`                  | Entities to generate code for                                                                         | List         | []                      |             |
| `skipForAnnotations`        | Skip generating operations for entities annotated with these                                          | List         | [vo, embedded, skip]    |             |
| `targetFile`                | Target file                                                                                           | String       | asyncapi.yml            |             |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) | String       | x-business-entity       |             |      
| `basePackage`               | Java Models package name                                                                              | String       | io.example.domain.model |             |
| `skipFormatting`            | Skip java sources output formatting                                                                   | boolean      | false                   |             |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIConfiguration --help
```
