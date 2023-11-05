# OpenAPI: REST Controllers Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates implementations based on JDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.

After you have generated SpringMVC interfaces and DTOs with OpenAPI generator, you can use this command to generate implementations (skeletons) and mappers for those interfaces and dtos:

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin    specFile=src/main/resources/model/openapi.yml \
    zdlFile=src/main/resources/model/orders-model.jdl \
    basePackage=io.zenwave360.example \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    targetFolder=.
```

## Options

| **Option**                           | **Description**                                                                                                                                                            | **Type**         | **Default**                                      | **Values**           |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|--------------------------------------------------|----------------------|
| `zdlFile`                            | JDL file to parse                                                                                                                                                          | String           |                                                  |                      |
| `specFile`                           | API Specification File                                                                                                                                                     | URI              |                                                  |                      |
| `targetFolder`                       | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                 | File             |                                                  |                      |
| `specFiles`                          | JDL files to parse                                                                                                                                                         | String[]         | [null]                                           |                      |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                      | String           | x-business-entity                                |                      |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)                                        | String           | x-business-entity-paginated                      |                      |
| `paginatedDtoItemsJsonPath`          | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List             | [$.properties.items, $.properties.content.items] |                      |
| `dtoToEntityNameMap`                 | Maps openapi dtos to jdl entity names                                                                                                                                      | Map              | {}                                               |                      |
| `controllersPackage`                 | The package to generate REST Controllers                                                                                                                                   | String           | {{basePackage}}.adapters.web                     |                      |
| `entitiesPackage`                    | Package where your domain entities are                                                                                                                                     | String           | {{basePackage}}.core.domain                      |                      |
| `inboundDtosPackage`                 | Package where your inbound dtos are                                                                                                                                        | String           | {{basePackage}}.core.inbound.dtos                |                      |
| `servicesPackage`                    | Package where your domain services/usecases interfaces are                                                                                                                 | String           | {{basePackage}}.core.inbound                     |                      |
| `inputDTOSuffix`                     | Suffix for CRUD operations DTOs (default: Input)                                                                                                                           | String           | Input                                            |                      |
| `entityDTOSuffix`                    | Suffix for (output) entities DTOs (default: empty to use the entity itself)                                                                                                | String           |                                                  |                      |
| `criteriaDTOSuffix`                  | Suffix for search criteria DTOs (default: Criteria)                                                                                                                        | String           | Criteria                                         |                      |
| `searchDTOSuffix`                    | Suffix for elasticsearch document entities (default: Document)                                                                                                             | String           | Document                                         |                      |
| `style`                              | Programming Style                                                                                                                                                          | ProgrammingStyle | imperative                                       | imperative, reactive |
| `basePackage`                        | Applications base package                                                                                                                                                  | String           |                                                  |                      |
| `openApiApiPackage`                  | The package to used by OpenAPI-Generator for generated api objects/classes                                                                                                 | String           |                                                  |                      |
| `openApiModelPackage`                | The package to used by OpenAPI-Generator for generated model objects/classes                                                                                               | String           | {{openApiApiPackage}}                            |                      |
| `openApiModelNamePrefix`             | Sets the prefix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                                  |                      |
| `openApiModelNameSuffix`             | Sets the suffix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                                  |                      |
| `operationIds`                       | OpenAPI operationIds to generate code for                                                                                                                                  | List             | []                                               |                      |
| `statusCodes`                        | Status codes to generate code for (default: 200, 201, 202 and 400                                                                                                          | List             | [200, 201, 202, 400]                             |                      |
| `skipFormatting`                     | Skip java sources output formatting                                                                                                                                        | boolean          | false                                            |                      |
| `haltOnFailFormatting`               | Halt on formatting errors                                                                                                                                                  | boolean          | true                                             |                      |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin --help
```