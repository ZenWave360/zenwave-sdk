## What's Changed

Patch release with fixes and compatibility improvements for AsyncAPI and OpenAPI processing.

## What's New

### AsyncAPI JSON Schema to POJO

- Fixed Java class-name normalization for schemas and `$ref` targets containing word delimiters such as underscores. For example, `Address_c` now generates `AddressC`. This also applies when `useTitleAsClassname` is enabled and when schemas are loaded from external JSON Schema files. ([#114](https://github.com/ZenWave360/zenwave-sdk/issues/114))
- Added support for generating POJOs from external JSON Schema references, including external definitions without an explicit schema format.
- Aligned annotator handling across schema-generation paths.

### API Overlays

- Added `apiOverlayFiles` support for applying ordered YAML overlays before dereferencing and `allOf` merging.
- Added overlay support across AsyncAPI, OpenAPI, JSON Schema to POJO, and AsyncAPI Ops generation paths.
- Improved overlay URI handling, including local files, file-backed `classpath:` resources, Windows file paths, and clearer I/O error behavior.

### Build and Parser Compatibility

- Updated the managed `maven-javadoc-plugin` to `3.12.0` and wired `maven-javadoc-plugin.version` consistently in plugin management. This avoids the blocked vulnerable transitive `commons-text:1.9` dependency used by the previous plugin version. ([#115](https://github.com/ZenWave360/zenwave-sdk/issues/115))
- Migrated JSON Schema reference processing to `json-schema-ref-parser-kmp` with a compatibility layer for the existing generator behavior.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.1...v2.5.2
