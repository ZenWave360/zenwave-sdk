# JDL To OpenAPI Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generate OpenAPI definition from JDL entities:

- Component Schemas for entities, plain and paginated lists
- CRUD operations for entities

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin \
    specFile=src/main/resources/model/orders-model.jdl \
    idType=integer \
    idTypeFormat=int64 \
    targetFile=src/main/resources/model/openapi.yml
```

## Options

| **Option**                  | **Description**                                                                                       | **Type**            | **Default**             | **Values**   |
|-----------------------------|-------------------------------------------------------------------------------------------------------|---------------------|-------------------------|--------------|
| `specFile`                  | Spec file to parse                                                                                    | String              |                         |              |
| `specFiles`                 | ZDL files to parse                                                                                    | String[]            | []                      |              |
| `asyncapiVersion`           | Target AsyncAPI version.                                                                              | AsyncapiVersionType | v3                      | v2, v3       |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat        | schema                  | schema, avro |
| `basePackage`               | Java Models package name                                                                              | String              | io.example.domain.model |              |
| `avroPackage`               | Package name for generated Avro Schemas (.avsc)                                                       | String              | io.example.domain.model |              |
| `idType`                    | JsonSchema type for id fields and parameters.                                                         | String              | string                  |              |
| `idTypeFormat`              | JsonSchema type format for id fields and parameters.                                                  | String              |                         |              |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) | String              | x-business-entity       |              |
| `targetFolder`              | Target folder to generate code to. If left empty, it will print to stdout.                            | File                |                         |              |
| `targetFile`                | Target file                                                                                           | String              | asyncapi.yml            |              |


## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin --help
```

# OpenAPI To JDL

Generates JDL model from OpenAPI schemas

## Options

| **Option**         | **Description**                                                            | **Type** | **Default**             | **Values** |
|--------------------|----------------------------------------------------------------------------|----------|-------------------------|------------|
| `specFile`         | API Specification File                                                     | URI      |                         |            |
| `targetFolder`     | Target folder to generate code to. If left empty, it will print to stdout. | File     |                         |            |
| `entities`         | Entities to generate code for                                              | List     | []                      |            |
| `targetFile`       | Target file                                                                | String   | entities.jdl            |            |
| `useRelationships` | Whether to use JDL relationships or plain field                            | boolean  | true                    |            |
| `basePackage`      | Java Models package name                                                   | String   | io.example.domain.model |            |


## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIToJDLPlugin --help
```
