# ZDL To OpenAPI Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generate OpenAPI definition from ZDL Models:

- Component Schemas for entities, plain and paginated lists
- CRUD operations for entities

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin \
    specFile=src/main/resources/model/orders-model.zdl \
    idType=integer \
    idTypeFormat=int64 \
    targetFile=src/main/resources/model/openapi.yml
```

## Options

| **Option**              | **Description**                                                                                                                    | **Type** | **Default** | **Values** |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------|----------|-------------|------------|
| `zdlFile`               | ZDL file to parse                                                                                                                  | String   |             |            |
| `zdlFiles`              | ZDL files to parse                                                                                                                 | List     | []          |            |
| `title`                 | API Title                                                                                                                          | String   |             |            |
| `targetFolder`          | Target folder to generate code to. If left empty, it will print to stdout.                                                         | File     |             |            |
| `targetFile`            | Target file                                                                                                                        | String   | openapi.yml |            |
| `idType`                | JsonSchema type for id fields and parameters.                                                                                      | String   | string      |            |
| `idTypeFormat`          | JsonSchema type format for id fields and parameters.                                                                               | String   |             |            |
| `dtoPatchSuffix`        | DTO Suffix used for schemas in PATCH operations                                                                                    | String   | Patch       |            |
| `operationIdsToInclude` | Operation IDs to include. If empty, all operations will be included. (Supports Ant-style wildcards)                                | List     |             |            |
| `operationIdsToExclude` | Operation IDs to exclude. If not empty it will be applied to the processed operationIds to include. (Supports Ant-style wildcards) | List     |             |            |
| `continueOnZdlError`    | Continue even when ZDL contains fatal errors                                                                                       | boolean  | true        |            |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin --help
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
