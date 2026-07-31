# Event Catalog Generator Summary

## What this module is

`event-catalog-generator` is a ZenWave SDK plugin that transforms a ZenWave architecture manifest plus service artifacts into an EventCatalog content tree.

Its current responsibility is:

- Load the retail architecture from `zenwave-architecture.yml`
- Resolve service docs and artifacts through `manifest-core`
- Parse AsyncAPI, OpenAPI, and ZDL inputs
- Generate EventCatalog MDX files under `target/event-catalog-test`
- Sync those generated files into the EventCatalog project fixture
- Build the local EventCatalog site with `eventcatalog build`

Frontmatter is generated from structured Java types. Each resource body is rendered from its own
overrideable Handlebars MDX template.

## Main pipeline

The plugin chain is defined in [EventCatalogPlugin.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogPlugin.java):

1. `EventCatalogArchitectureLoader`
2. `EventCatalogAsyncApiProcessor`
3. `EventCatalogOpenApiProcessor`
4. `EventCatalogZdlProcessor`
5. `EventCatalogGenerator`
6. `EventCatalogFileWriter`

### 1. Architecture loading

[EventCatalogArchitectureLoader.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogArchitectureLoader.java) loads the master manifest through `manifest-core` and stores:

- `manifest`
- `manifestRuntime`, the Java-friendly blocking JVM facade from `manifest-core`
- `eventCatalog`, which contains only mutable EventCatalog-specific enrichment

Hierarchy, service identity, documents, artifacts, and source resolution remain in the typed
manifest-core model; the plugin no longer creates a flattened copy of the manifest.

The manifest model exposes typed domains, subdomains, services, docs, artifacts, diagnostics, and
configured content sources. URI loading and source fallback are handled by `manifest-core`.

### 2. AsyncAPI processing

[EventCatalogAsyncApiProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogAsyncApiProcessor.java) loads `asyncapi` and `asyncapi-client` artifacts and enriches each service with:

- `_version`
- `_channels`
- `_events`
- `_commands`
- `_sends`
- `_receives`

It also computes:

- published message `schemaPath` values
- service artifact link metadata
- service artifact build-path metadata

### 3. OpenAPI processing

[EventCatalogOpenApiProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogOpenApiProcessor.java) loads `openapi` artifacts and enriches services with:

- `_queries`
- `_version` when available

It also computes schema links for external `$ref` response schemas.

### 4. ZDL processing

[EventCatalogZdlProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogZdlProcessor.java) loads `zdl` artifacts and enriches services with:

- `_entities`

This is the source for entity pages and entity relationships in the catalog.

### 5. MDX generation

[EventCatalogGenerator.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogGenerator.java) converts the typed manifest plus EventCatalog-only enrichment into MDX files.

It currently generates:

- domains
- subdomains
- services
- channels
- events
- commands
- queries
- entities

The generator is now aligned with EventCatalog’s expected directory structure:

- `domains/<domain>/index.mdx`
- `domains/<domain>/subdomains/<subdomain>/index.mdx`
- `domains/<domain>/subdomains/<subdomain>/services/<service>/index.mdx`
- sibling folders below that for `channels`, `events`, `commands`, `queries`, and `entities`

### 6. File writing

[EventCatalogFileWriter.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogFileWriter.java) writes the generated outputs into the target folder and handles cleanup/versioned preservation behavior used by the tests.

## Source hierarchy

The generator now supports hierarchical loading through `manifest-core`.

### Build-time content

Build-time content is what the generator parses.

Current intended default:

- `workspace-first`

That means:

- service docs
- AsyncAPI
- OpenAPI
- ZDL

are loaded from the local workspace when available.

This is controlled by:

- `preferredSource`
- `allowFallback`

Loading and reference resolution use the public `ZenWaveManifestLoader` API and its
`BlockingZenWaveManifestLoader` JVM facade from `manifest-core`.

### Published links

Published links are what appear in frontmatter for EventCatalog consumption.

Current split:

- message `schemaPath` can point to HTTP
- service `specifications[].path` must remain buildable by EventCatalog, so they are emitted as local relative paths for the local site build

This is important because EventCatalog tries to read service specification files from disk during site generation.

## Test manifest

The retail test manifest is here:

[zenwave-architecture.yml](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/zenwave-sdk-test-resources/src/main/resources/retail-domain-catalog/zenwave-architecture.yml)

It uses `contentResolution: [workspace, git]` and a generic Git source pointing to GitHub raw
content on `develop`.

So the test setup is ready for:

- local workspace-first generation now
- remote HTTP-first iteration later

## Generated output layout

The generator writes to:

- `target/event-catalog-test`

The EventCatalog project fixture consumes checked-in catalog content through:

- `event-catalog-project/scripts/sync-from-target.sh`

The importer fixture content lives under:

- `src/test/resources/event-catalog-content`

and then builds via:

- `npm run build:sync`

## What is correct now

The current state is good for structure and frontmatter:

- EventCatalog recognizes domains
- EventCatalog recognizes subdomains
- EventCatalog recognizes services
- messages, commands, queries, channels, and entities are generated
- the retail local site builds successfully

## Page body extension

Built-in `domain`, `subdomain`, `service`, `event`, `command`, `query`, `entity`, and `channel`
bodies are separate `.mdx.hbs` templates. Copy the same resource-relative template path below
`.zenwave/templates` to override one page type without changing the generator.

## Good next-step questions for the next session

If you want to iterate page content next, the useful questions are:

1. What should each page type contain beyond frontmatter?
2. Which sections should be generated from parsed artifacts versus handwritten templates?
3. Which docs from `service.docs` should be embedded on each page type?
4. Which sections should be short summaries versus full generated reference blocks?
5. Which page types should remain minimal because EventCatalog already renders enough from frontmatter?

## Useful files for the next iteration

- [EventCatalogGenerator.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogGenerator.java)
- [EventCatalogModel.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogModel.java)
- [EventCatalogAsyncApiProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogAsyncApiProcessor.java)
- [EventCatalogOpenApiProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogOpenApiProcessor.java)
- [EventCatalogZdlProcessor.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/main/java/io/zenwave360/sdk/plugins/EventCatalogZdlProcessor.java)
- [EventCatalogFrontmatterTest.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/test/java/io/zenwave360/sdk/plugins/EventCatalogFrontmatterTest.java)
- [EventCatalogGeneratorTest.java](C:/Users/ivangsa/workspace/zenwave/zenwave-sdk/plugins/event-catalog-generator/src/test/java/io/zenwave360/sdk/plugins/EventCatalogGeneratorTest.java)

## Recommended starting point for the next session

Start by defining the target body template for one page type only, preferably `service` or `event`.

Those types already have enough structured metadata to generate useful sections such as:

- summary
- specifications
- producers/consumers
- channels
- operations
- related entities
- embedded docs snippets

Once one type is stable, the same pattern can be applied to the rest.
