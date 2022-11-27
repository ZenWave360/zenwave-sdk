> 👉 ZenWave360 Helps You Create Software Easy to Understand

# JDL 2 Backend Application Generator

Generates a full backend application using a flexible hexagonal architecture

This is the long description

## JDL Extensions/Customizations

This generator supports the following JDL extensions:

- if any entity is annotated with @aggregate then the following table applies:

| **Annotation**                  | **Entity** | **@Persistence** | **Repository** | **Id** |
|:--------------------------------|------------|:-----------------|:---------------|:-------|
| **entity**                      | yes        | yes              |                | yes    |
| **@aggregate**                  | yes        | yes              | yes            | yes    |
| **@embedded**                   | yes        | yes              |                |        |
| **@vo**                         | yes        |                  |                |        |
| **@searchCriteria(entityName)** |            |                  |                |        |
| **@skip**                       | no         |                  |                |        |

**@searchCriteria(entityName)** is used to specify the entity name for the search criteria, if empty will take the same fields as the actual entity.
**@skip** entities used as search criteria should be marked with @skip

## Options

| **Option**             | **Description**                                                            | **Type**         | **Default**             | **Values**           |
|------------------------|----------------------------------------------------------------------------|------------------|-------------------------|----------------------|
| `specFile`             | Spec file to parse                                                         | String           |                         |                      |
| `targetFolder`         | Target folder to generate code to. If left empty, it will print to stdout. | File             |                         |                      |
| `specFiles`            | JDL files to parse                                                         | String[]         | []                      |                      |
| `entities`             | Entities to generate code for                                              | List             | []                      |                      |
| `persistence`          | Persistence                                                                | PersistenceType  | mongodb                 | mongodb, jpa         |
| `style`                | Programming Style                                                          | ProgrammingStyle | imperative              | imperative, reactive |
| `inputDTOSuffix`       | Suffix for CRUD operations DTOs (default: Input)                           | String           | Input                   |                      |
| `criteriaDTOSuffix`    | Suffix for search criteria DTOs (default: Criteria)                        | String           | Criteria                |                      |
| `searchDTOSuffix`      | Suffix for elasticsearch document entities (default: Document)             | String           | Document                |                      |
| `basePackage`          | Java Models package name                                                   | String           | io.example.domain.model |                      |
| `skipFormatting`       | Skip java sources output formatting                                        | boolean          | false                   |                      |
| `haltOnFailFormatting` | Halt on formatting errors                                                  | boolean          | true                    |                      |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultPlugin --help
```
