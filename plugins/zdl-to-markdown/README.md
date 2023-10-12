# Zdl To Markdown
> 👉 ZenWave360 Helps You Create Software Easy to Understand

Generates Markdown glossary from Zdl Models

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin \
    specFile=src/main/resources/model/customer-address.zdl \
    targetFile=src/main/resources/model/customer-address.md
```

## Options

| **Option**     | **Description**                    | **Type** | **Default**           | **Values** |
|----------------|------------------------------------|----------|-----------------------|------------|
| `specFile`     | Spec file to parse                 | String   |                       |            |
| `specFiles`    | ZDL files to parse                 | String[] | []                    |            |
| `targetFile`   | Target file                        | String   | zdl-model-glossary.md |            |
| `targetFolder` | Target folder for generated output | String   |                       |            |


## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin --help
```
