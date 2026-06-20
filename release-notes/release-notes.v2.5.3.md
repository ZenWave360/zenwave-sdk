## What's Changed

Patch release with fixes for AsyncAPI message extraction, producer header imports, and JSON Schema to POJO option handling.

## What's New

### AsyncAPI JSON Schema to POJO

- Fixed `jsonschema2pojo.initializeCollections=false` handling so generated collection fields are not initialized when the option is disabled. ([#122](https://github.com/ZenWave360/zenwave-sdk/issues/122))
- Fixed `jsonschema2pojo.useJodaLocalDates=false` handling so `format: date` fields can generate as `String` when no explicit `dateType` override is provided. Explicit `dateType` values are still honored. ([#123](https://github.com/ZenWave360/zenwave-sdk/issues/123))

### AsyncAPI Generators

- Fixed missing Java imports for producer header schemas that contain `format: date-time` properties, including generated `java.time.Instant` imports in Spring Cloud Stream producer interfaces. ([#119](https://github.com/ZenWave360/zenwave-sdk/issues/119))
- Fixed AsyncAPI v2 operation message normalization to avoid `ClassCastException` while extracting messages. ([#118](https://github.com/ZenWave360/zenwave-sdk/issues/118))

### Documentation

- Restored missing sections in the AsyncAPI generator README and aligned related Spring Cloud Stream documentation links.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.2...v2.5.3
