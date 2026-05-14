## What's Changed

* Merged 2.5.1 by @ivangsa

## What's New

- chore(asyncapi-jsonschema2pojo): upgrade `jsonschema2pojo` to `1.3.3`
- fix(asyncapi-jsonschema2pojo): expose missing upstream configuration options including `useInnerClassBuilders`, `includeGeneratedAnnotation`, `removeOldOutput`, and `fileFilter`
- fix(asyncapi-jsonschema2pojo): align `targetVersion` behavior with upstream so JDK 17+ builds emit the modern generated annotation path
- fix(asyncapi-jsonschema2pojo): support canonical `useJakartaValidation` while keeping deprecated `isUseJakartaValidation` as a backward-compatible alias
- chore(asyncapi-jsonschema2pojo): switch remaining `commons-lang` usage in the configuration wrapper to `commons-lang3`

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.0...v2.5.1
