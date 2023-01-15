# JDL To AsyncAPI Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generate AsyncAPI definition from JDL entities:

- One channel for each entity update events
- Messages and payloads for each entity Create/Update/Delete events (AVRO and AsyncAPI schema)

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIPlugin \
    includeCommands=true \
    specFile=src/main/resources/model/orders-model.jdl \
    idType=integer \
    idTypeFormat=int64 \
    annotations=aggregate \
    targetFile=src/main/resources/model/asyncapi.yml
```

## Options

| **Option**                  | **Description**                                                                                       | **Type**     | **Default**             | **Values**   |
|-----------------------------|-------------------------------------------------------------------------------------------------------|--------------|-------------------------|--------------|
| `specFile`                  | Spec file to parse                                                                                    | String       |                         |              |
| `specFiles`                 | JDL files to parse                                                                                    | String[]     | []                      |              |
| `basePackage`               | Java Models package name                                                                              | String       | io.example.domain.model |              |
| `targetFolder`              | Target folder for generated output                                                                    | String       |                         |              |
| `targetFile`                | Target file                                                                                           | String       | asyncapi.yml            |              |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat | schema                  | schema, avro |
| `entities`                  | Entities to generate code for                                                                         | List         | []                      |              |
| `skipEntities`              | Entities to skip code generation for                                                                  | List         | []                      |              |
| `annotations`               | Annotations to generate code for (ex. aggregate)                                                      | List         | []                      |              |
| `skipForAnnotations`        | Skip generating operations for entities annotated with these                                          | List         | [vo, embedded, skip]    |              |
| `includeEvents`             | Include channels and messages to publish domain events                                                | boolean      | true                    |              |
| `includeCommands`           | Include channels and messages to listen for async command requests                                    | boolean      | false                   |              |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) | String       | x-business-entity       |              |
| `avroPackage`               | Package name for generated Avro Schemas (.avsc)                                                       | String       | io.example.domain.model |              |
| `skipFormatting`            | Skip java sources output formatting                                                                   | boolean      | false                   |              |
| `haltOnFailFormatting`      | Halt on formatting errors                                                                             | boolean      | true                    |              |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIPlugin --help
```
