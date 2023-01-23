# JDL 2 Backend Application Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates a full backend application using a flexible hexagonal architecture.

Visit https://zenwave360.github.io/docs/zenwave-sdk/backend-application for complete documentation.

## Options

| **Option**                        | **Description**                                                                                                                              | **Type**         | **Default**             | **Values**                        |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------|-------------------------|-----------------------------------|
| `specFile`                        | Spec file to parse                                                                                                                           | String           |                         |                                   |
| `targetFolder`                    | Target folder to generate code to. If left empty, it will print to stdout.                                                                   | File             |                         |                                   |
| `specFiles`                       | ZDL files to parse                                                                                                                           | String[]         | []                      |                                   |
| `basePackage`                     | Java Models package name                                                                                                                     | String           | io.example.domain.model |                                   |
| `persistence`                     | Persistence                                                                                                                                  | PersistenceType  | mongodb                 | mongodb, jpa                      |
| `style`                           | Programming Style                                                                                                                            | ProgrammingStyle | imperative              | imperative, reactive              |
| `databaseType`                    | SQL database flavor                                                                                                                          | DatabaseType     | postgresql              | postgresql, mariadb               |
| `useLombok`                       | Use @Getter and @Setter annotations from Lombok                                                                                              | boolean          | false                   |                                   |
| `inputDTOSuffix`                  | If not empty, it will generate (and use) an `input` DTO for each entity used as command parameter                                            | String           |                         |                                   |
| `includeEmitEventsImplementation` | Whether to add IEntityEventProducer interfaces as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work. | boolean          | false                   |                                   |
| `entities`                        | Entities to generate code for                                                                                                                | List             | []                      |                                   |
| `formatter`                       | Code formatter implementation                                                                                                                | Formatters       | spring                  | google, palantir, spring, eclipse |
| `skipFormatting`                  | Skip java sources output formatting                                                                                                          | boolean          | false                   |                                   |
| `continueOnZdlError`              | Continue even when ZDL contains fatal errors                                                                                                 | boolean          | true                    |                                   |
| `haltOnFailFormatting`            | Halt on formatting errors                                                                                                                    | boolean          | true                    |                                   |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin --help
```
