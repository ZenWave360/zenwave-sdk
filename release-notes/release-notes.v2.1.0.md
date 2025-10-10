## What's Changed

* Merged 2.1.0 by @ivangsa in https://github.com/ZenWave360/zenwave-sdk/pull/45

## What's New

- Spring-Boot + Kotlin: this introduces the first mayor customization of `BackendApplicationDefaultPlugin`, `OpenAPIControllersPlugin` and `SpringWebTestClientPlugin`.
- `useSpringModulith` option in BackendApplicationDefaultPlugin: adds Spring Modulith `@NamedInterface` annotations to public service interfaces and events.
- feat: @MapsId option in relationship for clearer option name

Users that need to rely on previous v1.7.1, with jbang you can use the following command to install:

```shell
jbang alias add --fresh --force --name=zw v1_7_1@zenwave360/zenwave-sdk
```

This release also includes several bugfixes and features:

* 2025-08-10 75bc742b fix: fix dependencies in test pom.xml {{Ivan Garcia Sainz-Aja}}
* 2025-08-05 79260f46 fix: replace DockerComposeContainer with ComposeContainer {{Ivan Garcia Sainz-Aja}}
* 2025-08-04 242c6f65 feat: @MapsId option in relationship for clearer option name (supports legacy) {{Ivan Garcia Sainz-Aja}}
* 2025-07-22 bb4a9772 fix(InMemoryRepositories): populates relationship ids on save {{Ivan Garcia Sainz-Aja}}
* 2025-07-22 66dcccf8 fix(InMemoryRepositories): populates relationship ids on save {{Ivan Garcia Sainz-Aja}}
* 2025-07-08 c464403f fix (openapi controllers): array param type {{Ivan Garcia Sainz-Aja}}
* 2025-07-08 d2716975 fix: avro id type {{Ivan Garcia Sainz-Aja}}
* 2025-07-08 77ed963d fix: Help in json format {{Ivan Garcia Sainz-Aja}}
* 2025-07-08 d3a979c8 fix: skipEntityId for "embedded" and "abstract" entities (bug) {{Ivan Garcia Sainz-Aja}}  (HEAD -> features/kotlin)
* 2025-07-08 17861f4f fix: autoheader support with json pointer (bug) {{Ivan Garcia Sainz-Aja}}
* 2025-06-14 7c785066 feat: add support for Kotlin+SpringBoot ProjectTemplates {{Ivan Garcia Sainz-Aja}}
* 2025-06-30 32d1257d feat: useSpringModulith {{Ivan Garcia Sainz-Aja}}  (origin/features/useSpringModulith, features/useSpringModulith)
* 2024-06-20 5b15c8f0 refactors ProjectTemplates.java / Generator.java {{Ivan Garcia Sainz-Aja}}  (origin/develop)
* 2025-06-18 9911f5b0 fix(ServiceTest.hbs): escape input parameter type for generics {{Ivan Garcia Sainz-Aja}}
* 2025-06-01 95feb376 feat: add validation for empty ZDL models and update markdown generation logic {{Ivan Garcia Sainz-Aja}}
* 2025-05-07 d31cda53 fix: use formatted POJO name in converted json for javaType value {{Dmitrii Mikheev}}


**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.0.0-RC1...v2.1.0
