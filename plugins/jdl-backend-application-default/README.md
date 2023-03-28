# JDL 2 Backend Application Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generates a full backend application using a flexible hexagonal architecture

## JDL Extensions/Customizations

This generator supports the following JDL extensions:

### Extensions In JDL Language
- Field Types: in addition to enums and basic types it allows:
    - other entities as field type, this is useful for embedded fields which are not relations
    - array fields `fieldName String[]` or even `fieldName OtherEntity[]`
- Service: in addition to serviceClass and serviceImpl it allows configuring free text value as serviceName to allow grouping multiple entities in a given service. Then it's up to each generator to generate an interface or just an implementation class.

### Extensions With Annotations

- **@extends(entityName)**
- **@copy(entityName)**
- **@auditing**

- if any entity is annotated with @aggregate then the following table applies:

| **Annotation**                  | **Entity** | **Inbound DTO**      | **@Persistence** | **Repository** | **Id**   |
|:--------------------------------|------------|----------------------|:-----------------|:---------------|:---------|
| **entity**                      | yes        |                      | yes              |                | yes      |
| **@aggregate**                  | yes        |                      | yes              | yes            | yes      |
| **@embedded**                   | yes        |                      | yes              |                | only jpa |
| **@vo**                         | yes        |                      |                  |                |          |
| **@input**                      | no         | yes                  |                  |                |          |
| **@searchCriteria(entityName)** |            | yes (for entityName) |                  |                |          |
| **@skip**                       | no         |                      |                  |                |          |

- **@searchCriteria(entityName)** is used to specify the entity name for the search criteria, if empty will take the same fields as the actual entity.
- **@skip** entities used as search criteria should be marked with @skip

- **@dbref** fields will be mapped as mongodb `@DocumentedReference`

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
jbang zw -p io.zenwave360.sdk.plugins.JDLBackendApplicationDefaultPlugin --help
```
