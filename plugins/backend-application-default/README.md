# Backend Application Default Plugin

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
| `useSpringModulith`               | Whether to use Spring Modulith annotations and features                                                                                                              | boolean          | false                   |                                                                                                                                                            |
| `useJSpecify`                     | Whether to use JSpecify for nullability annotations                                                                                                                  | boolean          | false                       |                                                                                                                                                            |
| `useJMolecules`                   | Whether to add jMolecules DDD and architecture annotations to generated code                                         | boolean          | false                   |                                                                                                              |
| `addRelationshipsById`            | Controls whether to add a read/write relationship by id when mapping relationships between aggregate (not recommended) keeping the relationship by object readonly.  | boolean          | false                   |                                                                                                                                                            |
| `idJavaType`                      | Specifies the Java data type for the ID fields of entities. Defaults to Long for JPA and String for MongoDB if not explicitly set.                                   | String           |                         |                                                                                                                                                            |
| `includeEmitEventsImplementation` | Whether to add AsyncAPI/ApplicationEventPublisher as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work.                      | boolean          | true                    |                                                                                                                                                            |
| `targetFolder`                    | Target folder to generate code to. If left empty, it will print to stdout.                                                                                           | File             |                         |                                                                                                                                                            |
| `continueOnZdlError`              | Continue even when ZDL contains fatal errors                                                                                                                         | boolean          | true                    |                                                                                                                                                            |
| `formatter`                       | Code formatter implementation                                                                                                                                        | Formatters       | palantir                | palantir, spring, google                                                                                                                                   |
| `skipFormatting`                  | Skip java sources output formatting                                                                                                                                  | boolean          | false                   |                                                                                                                                                            |
| `haltOnFailFormatting`            | Halt on formatting errors                                                                                                                                            | boolean          | true                    |                                                                                                                                                            |

## jMolecules Annotations

Set `useJMolecules true` to annotate generated code with [jMolecules](https://jmolecules.org) DDD and
architecture stereotypes. Annotations are emitted as fully qualified names, so the SDK itself needs no
jMolecules dependency — only the generated project does:

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-ddd</artifactId>
</dependency>
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-events</artifactId>
</dependency>
<!-- plus jmolecules-hexagonal-architecture or jmolecules-layered-architecture, matching your layout -->
<dependency>
    <groupId>org.jmolecules.integrations</groupId>
    <artifactId>jmolecules-archunit</artifactId>
    <scope>test</scope>
</dependency>
```

The architecture vocabulary follows from `layout`, with no separate option to set:

| layout | architecture |
|---|---|
| `DefaultProjectLayout`, `CleanHexagonalProjectLayout`, `HexagonalProjectLayout` | `HEXAGONAL` |
| `LayeredProjectLayout` | `LAYERED` |
| `SimpleDomainProjectLayout`, `CleanArchitectureProjectLayout` | `NONE`, DDD annotations only |

What each generated artifact receives:

| artifact | DDD | HEXAGONAL | LAYERED |
|---|---|---|---|
| entity | `@AggregateRoot` or `@Entity`, `@Identity` on the id | | `@DomainLayer` |
| repository | `@Repository` | `@SecondaryPort` | `@InfrastructureLayer` |
| domain event (not `@asyncapi`) | `@DomainEvent` | | `@DomainLayer` |
| service interface | | `@PrimaryPort` | `@ApplicationLayer` |
| service implementation | | `@Application` | `@ApplicationLayer` |
| input / output DTO, `@vo` / `@embedded` entity | `@ValueObject` | | |
| controller, event listener, asyncapi consumer | | `@PrimaryAdapter` | `@InterfaceLayer` |
| event publisher port | | `@SecondaryPort` | |
| event publisher implementation | | `@SecondaryAdapter` | `@InfrastructureLayer` |
| Spring Modulith `package-info` | `@Module` | | |

`@vo` and `@embedded` entities are annotated `@ValueObject`, like inbound DTOs. `@asyncapi` events are
payload DTOs generated from the contract, so they are never `@DomainEvent`.

### Generated architecture test

A `JMoleculesArchitectureTest` is generated alongside, verifying the stereotypes with ArchUnit. Unlike
`ArchitectureTest`, which matches package names, these rules read the annotations, so they hold for any
layout. It is written once and never overwritten, so edits you make to it are permanent.

Two things worth knowing:

- `aggregateReferencesShouldBeViaIdOrAssociation` rejects any direct reference to an `@AggregateRoot`.
  A bidirectional relationship such as `Customer{addresses} to Address{customer}` generates exactly
  that, so the rule reports it. Comment out its `@ArchTest` to opt out.
- No `ensureLayering()` rule is generated for `LAYERED`. That rule encodes Evans' layering, where
  infrastructure sits below the domain and may not reference it, while `LayeredProjectLayout` is the
  three tier `web -> service -> repository`. A Spring Data repository names its aggregate in its own
  type signature, so the rule could never pass. The layer annotations still document the tiers.

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin --help
```

