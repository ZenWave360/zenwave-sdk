# ZDL To AsyncAPI Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generate OpenAPI definition from JDL entities:

- Component Schemas for entities, plain and paginated lists
- CRUD operations for entities

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin \
    specFile=src/main/resources/model/orders-model.zdl \
    idType=integer \
    idTypeFormat=int64 \
    targetFile=src/main/resources/model/openapi.yml
```

## Options

| **Option**                  | **Description**                                                                                       | **Type**            | **Default**             | **Values**   |
|-----------------------------|-------------------------------------------------------------------------------------------------------|---------------------|-------------------------|--------------|
| `specFile`                  | Spec file to parse                                                                                    | String              |                         |              |
| `targetFolder`              | Target folder to generate code to. If left empty, it will print to stdout.                            | File                |                         |              |
| `targetFile`                | Target file                                                                                           | String              | asyncapi.yml            |              |
| `asyncapiVersion`           | Target AsyncAPI version.                                                                              | AsyncapiVersionType | v3                      | v2, v3       |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat        | schema                  | schema, avro |
| `idType`                    | JsonSchema type for id fields and parameters.                                                         | String              | string                  |              |
| `idTypeFormat`              | JsonSchema type format for id fields and parameters.                                                  | String              |                         |              |
| `basePackage`               | Java Models package name                                                                              | String              | io.example.domain.model |              |
| `avroPackage`               | Package name for generated Avro Schemas (.avsc)                                                       | String              | io.example.domain.model |              |
| `zdlBusinessEntityProperty` | Extension property referencing original zdl entity in components schemas (default: x-business-entity) | String              | x-business-entity       |              |
| `continueOnZdlError`        | Continue even when ZDL contains fatal errors                                                          | boolean             | true                    |              |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin --help
```
