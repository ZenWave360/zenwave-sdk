# JDL To OpenAPI Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generate OpenAPI definition from JDL entities:

- Component Schemas for entities, plain and paginated lists
- CRUD operations for entities

```shell
jbang zw -p io.zenwave360.sdkns.JDLToOpenAPIPlugin \
    specFile=src/main/resources/model/orders-model.jdl \
    idType=integer \
    idTypeFormat=int64 \
    targetFile=src/main/resources/model/openapi.yml
```

## Options

| **Option**                           | **Description**                                                                                                                                                                 | **Type** | **Default**                           | **Values** |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------|------------|
| `specFile`                           | Spec file to parse                                                                                                                                                              | String   |                                       |            |
| `specFiles`                          | JDL files to parse                                                                                                                                                              | String[] | []                                    |            |
| `targetFolder`                       | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                      | File     |                                       |            |
| `targetFile`                         | Target file                                                                                                                                                                     | String   | openapi.yml                           |            |
| `basePackage`                        | Java Models package name                                                                                                                                                        | String   | io.example.domain.model               |            |
| `entities`                           | Entities to generate code for                                                                                                                                                   | List     | []                                    |            |
| `annotationsToGenerate`              | Skip generating operations for entities annotated with these                                                                                                                    | List     | [aggregate]                           |            |
| `skipForAnnotations`                 | Skip generating operations for entities annotated with these                                                                                                                    | List     | [vo, embedded, skip]                  |            |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                           | String   | x-business-entity                     |            |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists                                                                                    | String   | x-business-entity-paginated           |            |
| `paginatedDtoItemsJsonPath`          | JSONPath list to search for response DTO schemas for list or paginated results. Examples: '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List     | [$.items, $.properties.content.items] |            |
| `criteriaDTOSuffix`                  | Suffix for search criteria DTOs (default: Criteria)                                                                                                                             | String   | Criteria                              |            |
| `idType`                             | JsonSchema type for id fields and parameters.                                                                                                                                   | String   | string                                |            |
| `idTypeFormat`                       | JsonSchema type format for id fields and parameters.                                                                                                                            | String   |                                       |            |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdkns.JDLToOpenAPIPlugin --help
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
jbang zw -p io.zenwave360.sdkns.OpenAPIToJDLPlugin --help
```
