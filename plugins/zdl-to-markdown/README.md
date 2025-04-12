# Zdl To Markdown
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates Markdown glossary from Zdl Models

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
| `outputFormat`       | Template type                                | OutputFormat | glossary        | glossary, task_list, aggregate |
| `aggregateName`      | Aggregate name                               | String       |                 |                                |
| `skipDiagrams`       | Skip generating PlantUML diagrams            | boolean      | false           |                                |
| `targetFile`         | Target file                                  | String       | zdl-glossary.md |                                |
| `continueOnZdlError` | Continue even when ZDL contains fatal errors | boolean      | true            |                                |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin --help
```
