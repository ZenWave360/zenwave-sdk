## What's Changed

* Merged 2.0.0-RC1 by @ivangsa in https://github.com/ZenWave360/zenwave-sdk/pull/45

This release includes a major refactoring of code generation for advanced customization, introducing three new concepts:

- `ProjectLayout`: All plugins now support a `layout` property that points to a class extending `ProjectLayout`. The SDK provides several options: `CleanHexagonalProjectLayout`, `LayeredProjectLayout`, `SimpleDomainProjectLayout`, `HexagonalProjectLayout`, `CleanArchitectureProjectLayout` (default is `CleanHexagonalProjectLayout`). Additional custom layouts can be added to the classpath via jbang.
- `ProjectTemplates`: For advanced use cases, some plugins support a `templates` property that points to a class extending `ProjectTemplates`. The default is `BackendApplicationProjectTemplates`, which can be extended or customized.
- `GeneratedProjectFiles`: Provides structured output for generated files

These changes are transparent for users and it will help build tools around ZenWaveSDK and customize the experience for advanced usage.

Users that need to rely on previous version with jbang can use the following command:

```shell
jbang alias add --fresh --force --name=zw v1.7.1@zenwave360/zenwave-sdk
```

This release also includes several bugfixes and new features:

- Support for `@naturalId` in entities and service commands
- Support for `PATCH` operations with partial updates
- First-class support for modular monoliths
- Enhanced support for `Blobs` as entity field types
- Added file upload and download capabilities
- Added Spring Expression Language support for AsyncAPI `autoheaders`
- Added `CloudEvents` support in AsyncAPI generation from ZDL
- Enhanced support for `@delete` operations
- Added YAML merge and overlay support when generating OpenAPI/AsyncAPI from ZDL
- Fixed *Palantir* and *Google* Java formatters
- Added support for complex JSON objects as ZDL options
- Added automated E2E tests for all features

## Breaking Changes

Updated Maven groupId to `io.zenwave360.sdk`


**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v1.7.1...v2.0.0-RC1
