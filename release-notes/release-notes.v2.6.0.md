## What's Changed

Draft release notes for the upcoming **2.6.0** release.

## What's New

### AsyncAPI Traits and Semantic Navigation

- Integrated the lightweight `asyncapi-parser-kmp` module while preserving the existing ZenWave processor APIs.
- AsyncAPI trait processing now follows version-specific semantics in the parser library:
  - AsyncAPI v2 message and operation traits continue to be supported.
  - AsyncAPI v3 operation and message traits are processed according to the current specification.
  - AsyncAPI v3 channel traits support the proposed `x-traits` field and the forward-compatible `traits` field, with `x-traits` taking precedence whenever it is present.
  - Invalid traits are collected and reported as parser diagnostics without stopping generation.
- Parsed SDK models now retain their `AsyncApiDocument`, including effective/source views and declaration/usage provenance (`value`, `pointer`, `sourceUri`, and `usagePointer`).
- Added the version-neutral `operationMessages` Handlebars helper, backed directly by `AsyncApiDocument.operationMessages(operationId)`.
- Migrated both AsyncAPI generator families and message-schema utilities to semantic navigation instead of ad-hoc JSONPath collection. The internal `x--messages` derived property is **deprecated**: it is still populated on channels/operations (now sourced from `AsyncApiDocument` navigation) for backward compatibility with custom templates that read it directly, but the SDK's own templates and processors no longer read it and it may be removed in a future release.
- AsyncAPI v3 operations without an explicit `messages` list now correctly inherit messages declared by their referenced channel.
- Fixed the by-channel consumer template (`IServiceByChannel.java` + the shared `Headers` partial, used by the `asyncapi-generator` plugin) generating a duplicate, incorrectly-named `Headers` inner class per channel instead of one distinct `<Message>Headers` class per message — this broke compilation for any channel with more than one message type.
- Fixed several producer templates (`InMemoryEventsProducer`, transactional-outbox `Producer` variants, across `asyncapi-generator` and `asyncapi-spring-cloud-streams3`) silently skipping `processRuntimeHeaders(...)` generation due to passing the wrong context to the `hasRuntimeHeaders` helper.
- Updated `zdl-to-asyncapi` v3 message names to use `Command`, `Response`, or `Event` suffixes according to their role.

### Build & Dependencies

- Fixed `asyncapi-generator`/`avro-schema-compiler` declaring `org.apache.avro:avro-compiler` as a `test`/`provided`-scope dependency, which never propagated transitively. This made `org.apache.avro.Schema` unresolvable at Mojo class-loading time for **every** consumer of the `AsyncAPIGenerator` Maven goal, even projects that don't use Avro at all (the generator chain always includes an Avro compilation stage). `avro-compiler` is now a regular `compile`-scope (transitive) dependency, so consumers no longer need to manually redeclare `org.apache.avro:avro-compiler` (with Jackson exclusions) in their own POM just to work around this — see the updated `asyncapi-generator` [README](https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-generator/README.md#maven-usage).

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
