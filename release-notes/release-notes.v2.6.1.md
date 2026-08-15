## What's Changed

Patch release for the EventCatalog generator, focused on producer/consumer generation and business-flow generation based on `manifest-graph`.

## What's New

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

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.6.0...v2.6.1
