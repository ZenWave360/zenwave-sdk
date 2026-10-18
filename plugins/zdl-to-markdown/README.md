# Zdl To Markdown

Generates Markdown glossary from Zdl Models

For multi-page module documentation with Mermaid diagrams from ZDL, ZFL, OpenAPI and AsyncAPI, use the SDK CLI's
`DslDocs` plugin instead.

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin \
    specFile=src/main/resources/model/customer-address.zdl \
    targetFile=src/main/resources/model/customer-address.md
```

## Options

| **Option**           | **Description**                              | **Type**     | **Default**     | **Values**                     |
|----------------------|----------------------------------------------|--------------|-----------------|--------------------------------|
| `zdlFile`            | ZDL file to parse                            | String       |                 |                                |
| `zdlFiles`           | ZDL files to parse (comma separated)         | List         |                 |                                |
| `outputFormat`       | Template type                                | OutputFormat | glossary        | glossary, aggregate, plantuml  |
| `aggregateName`      | Aggregate name                               | String       |                 |                                |
| `targetFile`         | Target file                                  | String       | zdl-glossary.md |                                |
| `continueOnZdlError` | Continue even when ZDL contains fatal errors | boolean      | true            |                                |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin --help
```
