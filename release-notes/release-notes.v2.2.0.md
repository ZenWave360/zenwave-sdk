## What's Changed

## What's New

- Spring-Boot + Kotlin: idiomatic Kotlin with full e2e tests
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


**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.1.0...v2.2.0
