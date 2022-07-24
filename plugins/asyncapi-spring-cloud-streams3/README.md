# io.zenwave360.generator.plugins.SpringCloudStream3Configuration

Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI

${javadoc}

## Options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `specFile` | API Specification File | String |  |   |
| `targetFolder` | Target folder for generated output | String |  |   |
| `style` | Programming style | ProgrammingStyle | IMPERATIVE | IMPERATIVE, REACTIVE  |
| `exposeMessage` | Whether to expose underlying spring Message to consumers or not. Default: false | boolean | false |   |
| `apiPackage` | Java API package name | String | io.example.api |   |
| `modelPackage` | Java Models package name | String | io.example.api.model |   |
| `bindingTypes` | Binding names to include in code generation. Generates code for ALL bindings if left empty | List |  |   |
| `role` | Project role: PROVIDER\|CLIENT | RoleType | PROVIDER | PROVIDER, CLIENT  |
| `operationIds` | Operation ids to include in code generation. Generates code for ALL if left empty | List | [] |   |

## Getting Help

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringCloudStream3Configuration --help
```

