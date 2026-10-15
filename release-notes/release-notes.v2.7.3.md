## What's Changed

### Fixed: Multiline Avro Imports in AsyncAPI Generation

The AsyncAPI generator loads each `.avsc` file when `avroCompilerProperties.imports` is supplied as a comma-separated, multiline Maven property. This includes individual HTTP(S) URLs and a trailing comma. Previously, the entire property could be treated as one import path, leaving referenced Avro types undefined.

### Fixed: Maven Central Publishing Compatibility

Updated `central-publishing-maven-plugin` to `0.12.0` to address the Maven compatibility failure that prevented the 2.7.2 release from publishing.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.7.1...v2.7.3
