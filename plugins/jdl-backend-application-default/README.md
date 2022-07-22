# JDLBackendApplicationDefaultConfiguration

Generates a full backend application using a flexible hexagonal architecture

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration --help
```

## Options:

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFiles` | JDL files to parse |  |   |
| `entities` | Entities to generate code for |  |   |
| `persistence` | Persistence MONGODB|JPA default: MONGODB |  | mongodb  |
| `style` | ProgrammingStyle imperative|reactive default: imperative |  | imperative, reactive  |
| `inputDTOSuffix` | Suffix for CRUD operations DTOs (default: Input) |  |   |
| `criteriaDTOSuffix` | Suffix for search criteria DTOs (default: Criteria) |  |   |
| `searchDTOSuffix` | Suffix for elasticsearch document entities (default: Document) |  |   |
| `basePackage` | Java Models package name |  |   |
| `targetFolder` | Target folder for generated output |  |   |
| `specFile` | OpenAPI file to parse |  |   |
