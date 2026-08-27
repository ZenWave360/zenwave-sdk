## What's Changed

Draft release notes for the upcoming **2.7.0** release.

Includes EventCatalog producer/consumer and business-flow generation, plus ZDL listeners / AsyncAPI consumer adapters in `backend-application-default`.

## What's New

### ZDL Listeners and AsyncAPI Consumer Adapters

`backend-application-default` now generates inbound event listeners from ZDL `@listener` and referenced AsyncAPI consumer contracts. Design notes: [docs/zdl-listener-asyncapi-final-synthesis.md](../docs/zdl-listener-asyncapi-final-synthesis.md).

- Same-module `@listener({event: ...})` and cross-module `@listener(zdl: <ApiName>, event: ...)` against a referenced ZDL `apis {}` entry.
- Repeatable `@listener` bindings grouped one class per source, enabled independently by `implementEventListeners`.
- Internal listeners use Spring Modulith `@ApplicationModuleListener` (generated once, skip-overwrite).
- AsyncAPI consumer adapters implement separately generated `asyncapi-generator` contracts: regenerated mapper interface plus a generated-once MapStruct extension; `CUSTOM_REQUIRED` shapes get a compiling TODO body.
- Publishing is unchanged: backend services still call producer interfaces from a separate `asyncapi-generator` Maven execution.
- Java and Kotlin listener/adpaters included.

### EventCatalog Generator

#### Producer and Consumer Generation

- Generates EventCatalog producer and consumer relationships from AsyncAPI send/receive operations and API consumption declarations in the architecture manifest.
- Connects consumer services to the events, commands, and queries they exchange with provider services.
- Adds declared API consumers and message-flow metadata to generated service and resource pages.

#### Manifest-Graph-Based Flow Generation

- Generates EventCatalog business flows from ZFL artifacts resolved through `manifest-graph`.
- Projects actors, timers, operations, emitted events, responses, failures, compensation paths, loops, and terminal outcomes while retaining their graph-native identities and semantics.
- Uses validated graph identities and binding roles to reconcile commands and queries, preventing similarly named resources from being merged accidentally.
- Keeps operations without an invocation binding visible as internal flow nodes. Set `publishInternalOperations=true` to also publish their command or query pages.

#### Upgrade Note

- Modeled command and query page IDs now derive from the logical ZDL operation as `{service-id}.{operation-slug}`. AsyncAPI-backed commands previously used `{service-id}.{channel-slug}`; update affected EventCatalog URLs and references when upgrading.


### Test Suite Trim

Plugin tests no longer compile a full generated Spring Boot project for every ZDL variant. Nested Maven `test-compile` is kept for:

- one feature-complete JPA fixture (`customer-address-aggregate-and-entity-lifecycle.zdl`: rich aggregate + entity lifecycle, Java and Kotlin)
- the new cross-module listener compile
- Mongo persistence smokes, `@Disabled` by default (enable locally)

Other generator tests still generate and assert sources. Full compilation of layouts, persistence flavors, and Kotlin Mongo remains in e2e.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.6.0...HEAD
