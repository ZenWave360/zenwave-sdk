# JDLBackendApplicationDefaultConfiguration

Generates a full backend application using a flexible hexagonal architecture

This is the long description

## JDL Extensions/Customizations

| **Annotation**                  | **@Persistence** | **Repository** | **Id** |
|:--------------------------------|:-----------------|:---------------|:-------|
| **entity**                      | yes              |                | yes    |
| **@aggregate**                  | yes              | yes            | yes    |
| **@embedded**                   | yes              |                |        |
| **@vo**                         |                  |                |        |
| **@searchCriteria(entityName)** |                  |                |        |

## Options

| **Option**          | **Description**                                                            | **Type**         | **Default**             | **Values**           |
| ------------------- | -------------------------------------------------------------------------- | ---------------- | ----------------------- | -------------------- |
| `specFile`          | OpenAPI file to parse                                                      | String           |                         |                      |
| `targetFolder`      | Target folder to generate code to. If left empty, it will print to stdout. | File             |                         |                      |
| `specFiles`         | JDL files to parse                                                         | String[]         | [null]                  |                      |
| `entities`          | Entities to generate code for                                              | List             | []                      |                      |
| `persistence`       | Persistence                                                                | PersistenceType  | mongodb                 | mongodb              |
| `style`             | Programming Style                                                          | ProgrammingStyle | imperative              | imperative, reactive |
| `inputDTOSuffix`    | Suffix for CRUD operations DTOs (default: Input)                           | String           | Input                   |                      |
| `criteriaDTOSuffix` | Suffix for search criteria DTOs (default: Criteria)                        | String           | Criteria                |                      |
| `searchDTOSuffix`   | Suffix for elasticsearch document entities (default: Document)             | String           | Document                |                      |
| `basePackage`       | Java Models package name                                                   | String           | io.example.domain.model |                      |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration --help
```
