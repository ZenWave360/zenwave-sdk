## What's Changed

Draft release notes for the upcoming **2.6.0** release.

## What's New

### API Overlays

- Added Overlay 1.1 action semantics while preserving valid 1.0 overlays:
  - `1.0.x` and `1.1.x` versions are dispatched by their `major.minor` feature set.
  - Added the 1.1 `copy` action, including copy-and-remove rename workflows.
  - Added direct primitive updates and removals.
  - Added array append/concatenation and recursive object merge semantics with compatibility checks.
  - Added deterministic removal of multiple array elements without index-shift errors.
  - Actions now use `remove`, `update`, then `copy` precedence and are applied sequentially to the current transformed document.
  - Overlay application fully deep-copies the source document, including objects nested in arrays.
- Overlay 1.1 JSONPath expressions continue to use the existing Jayway JSONPath implementation. Jayway predates RFC 9535 and has syntax and semantic differences, so JSONPath support is legacy/best-effort and is not claimed as fully RFC 9535 compliant. Full RFC 9535 support is tracked in [#131](https://github.com/ZenWave360/zenwave-sdk/issues/131).
- Fixed object updates for root and wildcard targets so overlay properties are recursively merged without replacing existing content.
- Added authenticated HTTP(S) loading for merge and overlay resources applied to generated OpenAPI and AsyncAPI documents.
- Improved generated AsyncAPI YAML formatting by preserving key order, adding spacing between root sections, omitting the document marker and unnecessary quotes, and placing overlay-added `servers` after `info`.

### EventCatalog Import and Export

- Added bidirectional integration between [ZenWave Manifest](https://github.com/ZenWave360/zenwave-manifest) architecture manifests and [EventCatalog](https://www.eventcatalog.dev/):
  - Export a ZenWave `zenwave-architecture.yml` manifest—including services, domains, subdomains, OpenAPI and AsyncAPI specifications, and entities—into an EventCatalog content tree.
    - Generate pages for channels, events, commands, queries, and entities from AsyncAPI, OpenAPI, and ZDL artifacts.
    - Include service documentation, specification links, schema references, message relationships, and entity metadata in the generated EventCatalog content.
    - Preserve versioned EventCatalog content during regeneration.
  - Import an EventCatalog content tree into a `zenwave-architecture.yml` manifest.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.4...HEAD
