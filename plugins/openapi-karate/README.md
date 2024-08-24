# Spring WebTestClient Generator
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates test for KarateDSL based on OpenAPI and Arazzo specifications.

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIKaratePlugin \
    specFile=src/main/resources/model/openapi.yml \
    targetFolder=src/test/resources \
    testsPackage=io.zenwave360.example.adapters.web.tests \
    groupBy=service
```

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIKaratePlugin \
    specFile=src/main/resources/model/openapi.yml \
    targetFolder=src/test/resources \
    testsPackage=io.zenwave360.example.adapters.web.tests \
    groupBy=businessFlow \
    businessFlowTestName=CustomerCRUDTest \
    operationIds=createCustomer,getCustomer,updateCustomer,deleteCustomer
```

## Options

| **Option**                     | **Description**                                                              | **Type**    | **Default**                                              | **Values**                                |
|--------------------------------|------------------------------------------------------------------------------|-------------|----------------------------------------------------------|-------------------------------------------|
| `specFile`                     | API Specification File                                                       | URI         |                                                          |                                           |
| `targetFolder`                 | Target folder to generate code to. If left empty, it will print to stdout.   | File        |                                                          |                                           |
| `basePackage`                  | Applications base package                                                    | String      |                                                          |                                           |
| `testsPackage`                 | Package name for generated tests                                             | String      | {{basePackage}}.adapters.web.tests                       |                                           |
| `groupBy`                      | Generate test classes grouped by                                             | GroupByType | service                                                  | service, operation, partial, businessFlow |
| `operationIds`                 | OpenAPI operationIds to generate code for                                    | List        | []                                                       |                                           |
| `businessFlowTestName`         | Business Flow Test name                                                      | String      |                                                          |                                           |

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.OpenAPIKaratePlugin --help
```
