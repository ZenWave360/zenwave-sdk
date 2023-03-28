# Spring WebTestClient Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification.

```shell
jbang zw -p io.zenwave360.sdk.plugins.SpringWebTestClientPlugin \
    specFile=src/main/resources/model/openapi.yml \
    targetFolder=src/test/java \
    testsPackage=io.zenwave360.example.adapters.web.tests \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    groupBy=service
```

```shell
jbang zw -p io.zenwave360.sdk.plugins.SpringWebTestClientPlugin \
    specFile=src/main/resources/model/openapi.yml \
    targetFolder=src/test/java \
    testsPackage=io.zenwave360.example.adapters.web.tests \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    groupBy=businessFlow \
    businessFlowTestName=CustomerCRUDTest \
    operationIds=createCustomer,getCustomer,updateCustomer,deleteCustomer
```

## Options

| **Option**                     | **Description**                                                              | **Type**    | **Default**                                              | **Values**                                |
|--------------------------------|------------------------------------------------------------------------------|-------------|----------------------------------------------------------|-------------------------------------------|
| `specFile`                     | API Specification File                                                       | URI         |                                                          |                                           |
| `targetFolder`                 | Target folder to generate code to. If left empty, it will print to stdout.   | File        |                                                          |                                           |
| `testsPackage`                 | Package name for generated tests                                             | String      | {{basePackage}}.adapters.web.tests                       |                                           |
| `groupBy`                      | Generate test classes grouped by                                             | GroupByType | service                                                  | service, operation, partial, businessFlow |
| `testSuffix`                   | Class name suffix for generated test classes                                 | String      | IT                                                       |                                           |
| `businessFlowTestName`         | Business Flow Test name                                                      | String      |                                                          |                                           |
| `transactional`                | Annotate tests as @Transactional                                             | boolean     | true                                                     |                                           |
| `transactionalAnnotationClass` | @Transactional annotation class name                                         | String      | org.springframework.transaction.annotation.Transactional |                                           |
| `basePackage`                  | Applications base package                                                    | String      |                                                          |                                           |
| `openApiApiPackage`            | The package to used by OpenAPI-Generator for generated api objects/classes   | String      |                                                          |                                           |
| `openApiModelPackage`          | The package to used by OpenAPI-Generator for generated model objects/classes | String      | {{openApiApiPackage}}                                    |                                           |
| `openApiModelNamePrefix`       | Sets the prefix for model enums and classes used by OpenAPI-Generator        | String      |                                                          |                                           |
| `openApiModelNameSuffix`       | Sets the suffix for model enums and classes used by OpenAPI-Generator        | String      |                                                          |                                           |
| `operationIds`                 | OpenAPI operationIds to generate code for                                    | List        | []                                                       |                                           |
| `statusCodes`                  | Status codes to generate code for                                            | List        | [200, 201, 202, 400]                                     |                                           |
| `skipFormatting`               | Skip java sources output formatting                                          | boolean     | false                                                    |                                           |
| `haltOnFailFormatting`         | Halt on formatting errors                                                    | boolean     | true                                                     |                                           |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.SpringWebTestClientPlugin --help
```
