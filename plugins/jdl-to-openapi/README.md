# io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration

Generates a full OpenAPI definitions for CRUD operations from JDL models

${javadoc}

## Options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFile` | OpenAPI file to parse | String |  |   |
| `targetFolder` | Target folder for generated output | String |  |   |
| `specFiles` | JDL files to parse | String[] | [null] |   |
| `entities` | Entities to generate code for | List | [] |   |
| `targetFile` | Target file | String | openapi.yml |   |
| `jdlBusinessEntityProperty` | Extension property referencing original jdl entity in components schemas (default: x-business-entity) | String | x-business-entity |   |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated) | String | x-business-entity-paginated |   |
| `paginatedDtoItemsJsonPath` | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List | [$.items, $.properties.content.items] |   |
| `basePackage` | Java Models package name | String | io.example.domain.model |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration --help
```

# io.zenwave360.generator.plugins.OpenAPIToJDLConfiguration

Generates JDL model from OpenAPI schemas

${javadoc}

## Options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFile` | API Specification File | String |  |   |
| `targetFolder` | Target folder for generated output | String |  |   |
| `entities` | Entities to generate code for | List | [] |   |
| `targetFile` | Target file | String | entities.jdl |   |
| `useRelationships` | Whether to use JDL relationships or plain field | boolean | true |   |
| `basePackage` | Java Models package name | String | io.example.domain.model |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.OpenAPIToJDLConfiguration --help
```

