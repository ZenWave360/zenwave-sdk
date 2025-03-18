# Backend Application Default Plugin
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates a full backend application using the provided 'layout' property

Sample configuration:

```zdl
config {
    basePackage "com.example"
    persistence jpa
    databaseType postgresql
    layout CleanHexagonalProjectLayout

    // The IDE will automatically use the active .zdl file
    // Alternatively, specify the path here to maintain separation between models and plugins
    zdlFile "models/example.zdl"

    plugins {
        BackendApplicationDefaultPlugin {
            useLombok true
            --force // overwrite all files
        }
    }
}
```

Visit https://www.zenwave360.io/docs/zenwave-sdk/backend-application for complete documentation.



## Options

| **Option**                        | **Description**                                                                                                                                                      | **Type**         | **Default**             | **Values**                                                                                                                                                 |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `layout`                          | Project organization and package structure [(documentation)](https://github.com/zenwave360/zenwave-sdk/tree/main/plugins/backend-application-default#project-layout) | ProjectLayout    | DefaultProjectLayout    | DefaultProjectLayout, CleanHexagonalProjectLayout, LayeredProjectLayout, SimpleDomainProjectLayout, HexagonalProjectLayout, CleanArchitectureProjectLayout |        
| `zdlFile`                         | ZDL file to parse                                                                                                                                                    | String           |                         |                                                                                                                                                            |
| `zdlFiles`                        | ZDL files to parse (comma separated)                                                                                                                                 | List             |                         |                                                                                                                                                            |
| `basePackage`                     | Java Models package name                                                                                                                                             | String           | io.example.domain.model |                                                                                                                                                            |
| `persistence`                     | Persistence                                                                                                                                                          | PersistenceType  | mongodb                 | mongodb, jpa                                                                                                                                               |
| `databaseType`                    | SQL database flavor                                                                                                                                                  | DatabaseType     | postgresql              | generic, postgresql, mysql, mariadb, oracle                                                                                                                |
| `style`                           | Programming Style                                                                                                                                                    | ProgrammingStyle | imperative              | imperative, reactive                                                                                                                                       |
| `useLombok`                       | Use @Getter and @Setter annotations from Lombok                                                                                                                      | boolean          | false                   |                                                                                                                                                            |
| `addRelationshipsById`            | Controls whether to add a read/write relationship by id when mapping relationships between aggregate (not recommended) keeping the relationship by object readonly.  | boolean          | false                   |                                                                                                                                                            |
| `idJavaType`                      | Specifies the Java data type for the ID fields of entities. Defaults to Long for JPA and String for MongoDB if not explicitly set.                                   | String           |                         |                                                                                                                                                            |
| `includeEmitEventsImplementation` | Whether to add AsyncAPI/ApplicationEventPublisher as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work.                      | boolean          | true                    |                                                                                                                                                            |
| `targetFolder`                    | Target folder to generate code to. If left empty, it will print to stdout.                                                                                           | File             |                         |                                                                                                                                                            |
| `continueOnZdlError`              | Continue even when ZDL contains fatal errors                                                                                                                         | boolean          | true                    |                                                                                                                                                            |
| `formatter`                       | Code formatter implementation                                                                                                                                        | Formatters       | palantir                | palantir, spring, google                                                                                                                                   |
| `skipFormatting`                  | Skip java sources output formatting                                                                                                                                  | boolean          | false                   |                                                                                                                                                            |
| `haltOnFailFormatting`            | Halt on formatting errors                                                                                                                                            | boolean          | true                    |                                                                                                                                                            |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin --help
```

