# OpenAPI: REST Controllers Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates implementations based on JDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.

After you have generated SpringMVC interfaces and DTOs with OpenAPI generator, you can use this command to generate implementations (skeletons) and mappers for those interfaces and dtos:

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin    
    openapiFile=src/main/resources/model/openapi.yml \
    zdlFile=src/main/resources/model/orders-model.jdl \
    basePackage=io.zenwave360.example \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    targetFolder=.
```

## Options

| **Option**                  | **Description**                                                                                                                                                            | **Type**         | **Default**                           | **Values**                        |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|---------------------------------------|-----------------------------------|
| `openapiFile`               | OpenAPI Specification File                                                                                                                                                 | String           |                                       |                                   |
| `zdlFile`                   | ZDL file to parse                                                                                                                                                          | String           |                                       |                                   |
| `targetFolder`              | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                 | File             |                                       |                                   |
| `style`                     | Programming Style                                                                                                                                                          | ProgrammingStyle | imperative                            | imperative, reactive              |
| `operationIds`              | OpenAPI operationIds to generate code for                                                                                                                                  | List             | []                                    |                                   |
| `statusCodes`               | Status codes to generate code for                                                                                                                                          | List             | [200, 201, 202, 400]                  |                                   |
| `openApiApiPackage`         | The package to used by OpenAPI-Generator for generated api objects/classes                                                                                                 | String           |                                       |                                   |
| `openApiModelPackage`       | The package to used by OpenAPI-Generator for generated model objects/classes                                                                                               | String           | {{openApiApiPackage}}                 |                                   |
| `openApiModelNamePrefix`    | Sets the prefix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                       |                                   |
| `openApiModelNameSuffix`    | Sets the suffix for model enums and classes used by OpenAPI-Generator                                                                                                      | String           |                                       |                                   |
| `basePackage`               | Applications base package                                                                                                                                                  | String           |                                       |                                   |
| `controllersPackage`        | The package to generate REST Controllers                                                                                                                                   | String           | {{basePackage}}.adapters.web          |                                   |
| `entitiesPackage`           | Package where your domain entities are                                                                                                                                     | String           | {{basePackage}}.core.domain           |                                   |
| `inboundDtosPackage`        | Package where your inbound dtos are                                                                                                                                        | String           | {{basePackage}}.core.inbound.dtos     |                                   |
| `servicesPackage`           | Package where your domain services/usecases interfaces are                                                                                                                 | String           | {{basePackage}}.core.inbound          |                                   |
| `inputDTOSuffix`            | Should use same value configured in BackendApplicationDefaultPlugin. Whether to use an input DTO for entities used as command parameter.                                   | String           |                                       |                                   |
| `paginatedDtoItemsJsonPath` | JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results. | List             | [$.items, $.properties.content.items] |                                   |
| `formatter`                 | Code formatter implementation                                                                                                                                              | Formatters       | spring                                | google, palantir, spring, eclipse |
| `skipFormatting`            | Skip java sources output formatting                                                                                                                                        | boolean          | false                                 |                                   |
| `haltOnFailFormatting`      | Halt on formatting errors                                                                                                                                                  | boolean          | true                                  |                                   |
| `continueOnZdlError`        | Continue even when ZDL contains fatal errors                                                                                                                               | boolean          | true                                  |                                   |


## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin --help
```
