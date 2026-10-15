## What's Changed

### Fixed: Multiline Avro Imports in AsyncAPI Generation

The AsyncAPI generator now loads each `.avsc` file when `avroCompilerProperties.imports` is supplied as a comma-separated, multiline Maven property. This includes individual HTTP(S) URLs and a trailing comma. Previously, the entire property could be treated as one import path, leaving referenced Avro types undefined.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.7.1...v2.7.2
