# io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoConfiguration

Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files

${javadoc}

## Options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFile` | API Specification File | String |  |   |
| `targetFolder` | Target folder to generate code to. If left empty, it will print to stdout. | File |  |   |
| `messageNames` | Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty | List | [] |   |
| `jsonschema2pojo` | JsonSchema2Pojo settings | Map | {} |   |
| `apiPackage` | Java API package name | String | io.example.api |   |
| `modelPackage` | Java Models package name | String | io.example.api.model |   |
| `bindingTypes` | Binding names to include in code generation. Generates code for ALL bindings if left empty | List |  |   |
| `role` | Project role: PROVIDER\|CLIENT | RoleType | PROVIDER | PROVIDER, CLIENT  |
| `operationIds` | Operation ids to include in code generation. Generates code for ALL if left empty | List | [] |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.AsyncApiJsonSchema2PojoConfiguration --help
```

