# JDL To AsyncAPI Generator (io.zenwave360.sdk.plugins.JDLToAsyncAPIPlugin)
> ZenWave360 👉 Create Software Easy to Understand

Generates a full AsyncAPI definitions for CRUD operations from JDL models

- One channel for each entity update events
- Messages and payloads for each entity:
    - Supported Schema Formats: AVRO and AsyncAPI schema
    - Supported Payload Styles: Entity and State Transfer (for Create/Update/Delete events)

JDL Example:

```jdl
@aggregate
entity Customer {
  username String required minlength(3) maxlength(250)
  password String required minlength(3) maxlength(250)
  email String required minlength(3) maxlength(250)
  firstName String required minlength(3) maxlength(250)
  lastName String required minlength(3) maxlength(250)
}
```

Then run:

```shell
jbang zw -p io.zenwave360.sdk.plugins.JDLToAsyncAPIPlugin \
    includeCommands=true \
    specFile=src/main/resources/model/orders-model.jdl \
    idType=integer \
    idTypeFormat=int64 \
    annotations=aggregate \
    payloadStyle=event \
    targetFile=src/main/resources/model/asyncapi.yml
```


## Options

| **Option**           | **Description**                                                    | **Type**            | **Default**             | **Values**    |
|----------------------|--------------------------------------------------------------------|---------------------|-------------------------|---------------|
| `zdlFile`            | ZDL file to parse                                                  | String              |                         |               |
| `zdlFiles`           | ZDL files to parse (comma separated)                               | List                |                         |               |
| `basePackage`        | Java Models package name                                           | String              | io.example.domain.model |               |
| `avroPackage`        | Package name for generated Avro Schemas (.avsc)                    | String              | io.example.domain.model |               |
| `schemaFormat`       | Schema format for messages' payload                                | SchemaFormat        | schema                  | schema, avro  |
| `includeEvents`      | Include channels and messages to publish domain events             | boolean             | true                    |               |
| `includeCommands`    | Include channels and messages to listen for async command requests | boolean             | false                   |               |
| `idType`             | JsonSchema type for id fields and parameters.                      | String              | string                  |               |
| `idTypeFormat`       | JsonSchema type format for id fields and parameters.               | String              |                         |               |
| `payloadStyle`       | Payload Style for messages' payload                                | PayloadStyle        | entity                  | entity, event |
| `asyncapiVersion`    | Target AsyncAPI version.                                           | AsyncapiVersionType | v2                      | v2, v3        |
| `targetFile`         | Target file                                                        | String              | asyncapi.yml            |               |
| `targetFolder`       | Target folder for generated output                                 | String              |                         |               |
| `continueOnZdlError` | Continue even when ZDL contains fatal errors                       | boolean             | true                    |               |
| `entities`           | Entities to generate code for                                      | List                | []                      |               |
| `skipEntities`       | Entities to skip code generation for                               | List                | []                      |               |
| `annotations`        | Annotations to generate code for (ex. aggregate)                   | List                | []                      |               |
| `skipForAnnotations` | Skip generating operations for entities annotated with these       | List                | [vo, embedded, skip]    |               |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.JDLToAsyncAPIPlugin --help
```

