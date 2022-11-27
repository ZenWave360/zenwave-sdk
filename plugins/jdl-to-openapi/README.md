> 👉 ZenWave360 Helps You Create Software Easy to Understand

# JDL To OpenAPI Generator

Generates a full OpenAPI definitions for CRUD operations from JDL models

## Options

| **Option**                           | **Description**                                                                                                                                                                 | **Type** | **Default**                           | **Values** |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------|------------|
| `specFile`                           | Spec file to parse                                                                                                                                                              | String   |                                       |            |
| `targetFolder`                       | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                      | File     |                                       |            |
| `specFiles`                          | JDL files to parse                                                                                                                                                              | String[] | []                                    |            |
| `entities`                           | Entities to generate code for                                                                                                                                                   | List     | []                                    |            |
| `annotationsToGenerate`              | Skip generating operations for entities annotated with these                                                                                                                    | List     | [aggregate]                           |            |
| `skipForAnnotations`                 | Skip generating operations for entities annotated with these                                                                                                                    | List     | [vo, embedded, skip]                  |            |
| `targetFile`                         | Target file                                                                                                                                                                     | String   | openapi.yml                           |            |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                           | String   | x-business-entity                     |            |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists                                                                                    | String   | x-business-entity-paginated           |            |
| `paginatedDtoItemsJsonPath`          | JSONPath list to search for response DTO schemas for list or paginated results. Examples: '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List     | [$.items, $.properties.content.items] |            |
| `criteriaDTOSuffix`                  | Suffix for search criteria DTOs (default: Criteria)                                                                                                                             | String   | Criteria                              |            |
| `idType`                             | JsonSchema type for id fields and parameters.                                                                                                                                   | String   | string                                |            |
| `idTypeFormat`                       | JsonSchema type format for id fields and parameters.                                                                                                                            | String   |                                       |            |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIPlugin --help
```

#OpenAPIToJDLPlugin

Generates JDL model from OpenAPI schemas

${javadoc}

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
jbang zw -p io.zenwave360.generator.plugins.OpenAPIToJDLPlugin --help
```
