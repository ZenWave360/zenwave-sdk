# JDLBackendApplicationDefaultConfiguration

Generates a full backend application using a flexible hexagonal architecture

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration --help
```

## Options:

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFiles` | JDL files to parse | String[] | [Ljava.lang.String;@54eb2b70 |   |
| `entities` | Entities to generate code for | List | [] |   |
| `persistence` | Persistence | PersistenceType | mongodb | mongodb  |
| `style` | Programming Style | ProgrammingStyle | imperative | imperative, reactive  |
| `inputDTOSuffix` | Suffix for CRUD operations DTOs (default: Input) | String | Input |   |
| `criteriaDTOSuffix` | Suffix for search criteria DTOs (default: Criteria) | String | Criteria |   |
| `searchDTOSuffix` | Suffix for elasticsearch document entities (default: Document) | String | Document |   |
| `basePackage` | Java Models package name | String | io.example.domain.model |   |
| `targetFolder` | Target folder for generated output | String |  |   |
| `specFile` | OpenAPI file to parse | String |  |   |


