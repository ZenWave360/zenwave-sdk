## What’s Changed

- Merged release **2.3.0** by @ivangsa  
  https://github.com/ZenWave360/zenwave-sdk/pull/78

## What’s New

- Replaced `ZdlParser` dependency from  
  [zdl-jvm](https://github.com/ZenWave360/zdl-jvm) to  
  [dsl-kotlin-jvm](https://github.com/ZenWave360/dsl-kotlin)
- ZDL now supports optional / nullable command parameters
- Added `email` validation support in ZDL and `BackendApplicationDefaultPlugin`
- CLI dependencies can now be loaded via `@import` in `.zw` files, allowing replacement or override of existing classes and resources
- Added `useJSpecify` (default: false) option to `BackendApplicationProjectTemplates` to generate `@NullMarked` annotations
- Refactored ZDL-to-Markdown output to support ZenWaveDomainEditor
- Multiple bug fixes and improvements across Java and Kotlin templates

**Full Changelog**:  
https://github.com/ZenWave360/zenwave-sdk/compare/v2.2.2...v2.3.0
