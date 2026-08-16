# Event Catalog Generator

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

Generates an [EventCatalog](https://www.eventcatalog.dev/) source tree from a `zenwave-architecture.yml` master file.

The generator loads that manifest with [zenwave-manifest](https://github.com/ZenWave360/zenwave-manifest) and turns the artifacts it points at — Markdown, AsyncAPI, OpenAPI, ZDL, and ZFL — into EventCatalog MDX pages.

## Command line usage

Local workspace (sibling service checkouts):

```shell
jbang zw -p io.zenwave360.sdk.plugins.EventCatalogPlugin \
  inputFile=zenwave-architecture.yml \
  preferredSource=workspace \
  outputFolder=event-catalog-content
```

Remote manifest, Git-only sources (CI):

```shell
jbang zw -p io.zenwave360.sdk.plugins.EventCatalogPlugin \
  inputFile=https://raw.githubusercontent.com/arcadia-editions/arcadia-editions-architecture/main/zenwave-architecture.yml \
  preferredSource=git \
  allowFallback=false \
  linkSource=git \
  outputFolder=event-catalog-content
```

## Architecture manifest

The input is a `zenwave-architecture.yml` master file: an index of domains, subdomains, services, and the artifacts each service already owns. It does not copy OpenAPI, AsyncAPI, or ZDL content. It only points at those files and says how they fit together.

[zenwave-manifest](https://github.com/ZenWave360/zenwave-manifest) publishes the JSON Schema, resolves each pointer from configured sources (`workspace`, `git`, `apicurio`, `artifactory`, `maven`, …), and fetches the content the generator parses.

- Schema: [https://schemas.zenwave360.io/zenwave-architecture/latest/schema.json](https://schemas.zenwave360.io/zenwave-architecture/latest/schema.json)
- Live example: [arcadia-editions/arcadia-editions-architecture/zenwave-architecture.yml](https://github.com/arcadia-editions/arcadia-editions-architecture/blob/main/zenwave-architecture.yml)

```yaml
# yaml-language-server: $schema=https://schemas.zenwave360.io/zenwave-architecture/latest/schema.json

config:
  title: "Arcadia Editions - Event-Driven Retail Architecture"
  version: 0.0.1
  groupIdExpression: "com.arcadiaeditions.${owner.id}"
  artifactIdExpression: "${artifact.fileNameWithoutExtension}"
  contentResolution:
    - workspace
    - git
  sources:
    workspace:
      basePathExpression: "../${owner.repository}"
    git:
      provider: github
      server: "https://github.com"
      contentUrlExpression: "${server}/arcadia-editions/${owner.repository}/raw/main/${content.path}"

domains:
  "orders":
    id: "orders"
    name: "Orders"
    description: "Commercial order creation, confirmation, and cancellation"
    subdomains:
      "checkout":
        id: "orders.checkout"
        name: "Checkout"
        services:
          "orders-checkout":
            id: "orders.checkout.orders-checkout"
            repository: "orders-checkout-api"
            version: "0.0.0"
            name: "Orders Checkout"
            description: "Owns checkout flow, order lifecycle, and the handoff from purchase intent to confirmed order"
            docs:
              summary: SUMMARY.md
              content: EVENT_CATALOG.md
              changelog: CHANGELOG.md
            artifacts:
              - type: zdl
                path: "domain-model.zdl"
                version: "0.0.0"
              - type: asyncapi
                path: "asyncapi.yml"
                version: "0.1.1"
              - type: asyncapi-client
                path: "asyncapi-client.yml"
                version: "0.1.1"
              - type: openapi
                path: "openapi.yml"
                version: "0.0.0"
            consumers:
              - "payments.payment-processing.payments-processing#asyncapi-client"
              - "fulfillment.shipping.fulfillment-shipping#asyncapi-client"
```

Read from the top down: a domain contains subdomains, a subdomain owns services, and each service points at the contracts and documents that live in its own repository. Repeat that for every domain and the whole architecture sits in one file.

`contentResolution` is the ordered source list. Locally, `workspace` reads sibling checkouts from `sources.workspace.basePathExpression`. In a pipeline, the same entries resolve against Git (or Apicurio, Maven, …) without changing the manifest. `preferredSource` and `allowFallback` select which of those configured sources the generator uses for this run.

`service.docs` is rendered into the service page body. The default template concatenates `summary`, `content`, and `changelog`. `consumers` list the services — and the exact artifact, when the `#artifactId` selector is present — that consume this service's contracts.

Domain-level `zfl` artifacts become EventCatalog flows under that domain.

## Configuration options

| **Option** | **Description** | **Type** | **Default** | **Values** |
|------------|-----------------|----------|-------------|------------|
| `inputFile` | URI of the zenwave-architecture.yml master file. | URI | `null` |   |
| `outputFolder` | Output folder for the EventCatalog source tree. | String | `null` |   |
| `docsTemplate` | Custom Handlebars template for docs body rendering. | String | `null` |   |
| `preferredSource` | Preferred artifact source for build-time content loading. | String | `null` |   |
| `allowFallback` | Allow source fallback for build-time content loading. | Boolean | `null` |   |
| `linkSource` | Preferred source for generated frontmatter links. | String | `null` |   |
| `publishInternalOperations` | Publish unbound ZDL operations as synthesized EventCatalog command/query pages. | Boolean | `null` |   |
| `authentication` | Authentication configuration values for fetching remote resources. | List | `[]` |   |
| `targetFolder` | Target folder to generate code to. | File | `null` |   |

`outputFolder` is the public option; the plugin copies it to `targetFolder` for the file writer.

`preferredSource` / `allowFallback` control where artifacts and service docs are loaded from. `linkSource` controls the URLs written into generated frontmatter. The Arcadia CI run uses `preferredSource=git`, `allowFallback=false`, and `linkSource=git` so the catalog never reads a local checkout.

Use `authentication` when the manifest or its artifacts need authenticated HTTP access.

## What it generates

One run writes a complete EventCatalog content tree under `outputFolder`. Frontmatter is built from typed Java records and serialized as YAML. The MDX body of each page is rendered from a Handlebars template.

| Path | Source |
|------|--------|
| `domains/{domain}/index.mdx` | Manifest domain |
| `domains/{domain}/subdomains/{subdomain}/index.mdx` | Manifest subdomain |
| `domains/{domain}/subdomains/{subdomain}/services/{service}/index.mdx` | Service + `service.docs` |
| `.../channels/{channel}/index.mdx` | AsyncAPI channels |
| `.../events/{id}/index.mdx` | AsyncAPI events |
| `.../commands/{id}/index.mdx` | AsyncAPI commands and bound ZDL operations |
| `.../queries/{id}/index.mdx` | OpenAPI `GET` / `HEAD` operations |
| `.../entities/{id}/index.mdx` | ZDL entities |
| `domains/{domain}/flows/{flow}/index.mdx` | Domain-level ZFL artifacts |

The writer replaces generated files and archives a previous revision under `versioned/` when the frontmatter version changes. Existing `versioned/` trees are preserved, so the catalog can keep history across regenerations.

## Templates

Page bodies are Handlebars templates. Built-in files live under
`io/zenwave360/sdk/plugins/EventCatalogGenerator`:

- `domain.mdx.hbs`
- `subdomain.mdx.hbs`
- `service.mdx.hbs`
- `channel.mdx.hbs`
- `event.mdx.hbs`
- `command.mdx.hbs`
- `query.mdx.hbs`
- `entity.mdx.hbs`
- `flow.mdx.hbs`
- `docs.md.hbs`

As with the other ZenWave generators, copy the same relative path below `.zenwave/templates` to override one template without changing the plugin:

```text
.zenwave/templates/io/zenwave360/sdk/plugins/EventCatalogGenerator/service.mdx.hbs
```

`docsTemplate` replaces only the service-docs body template. It receives a map of `{ key → file content }` for the keys declared in `service.docs`, and defaults to `docs.md.hbs`, which concatenates `summary`, `content`, and `changelog`.

Frontmatter is not templated. Override a `.mdx.hbs` file to change the page body, not the YAML header.

## Live pipeline

A complete generate → verify → publish pipeline lives in [arcadia-editions/arcadia-event-catalog](https://github.com/arcadia-editions/arcadia-event-catalog):

- Architecture manifest: [arcadia-editions-architecture/zenwave-architecture.yml](https://github.com/arcadia-editions/arcadia-editions-architecture/blob/main/zenwave-architecture.yml)
- Workflow: [`.github/workflows/update-catalog.yml`](https://github.com/arcadia-editions/arcadia-event-catalog/blob/main/.github/workflows/update-catalog.yml)
- Generator invocation: [`scripts/generate-catalog.sh`](https://github.com/arcadia-editions/arcadia-event-catalog/blob/main/scripts/generate-catalog.sh)
- Published site: [https://arcadia-editions.github.io/arcadia-event-catalog/](https://arcadia-editions.github.io/arcadia-event-catalog/)

The workflow checks out the catalog repo, installs a pinned JBang + ZenWave alias, runs the command above against the raw GitHub URL of the architecture manifest, then verifies, builds, and merges the regenerated `event-catalog-content/` tree. Generated MDX is kept separate from the EventCatalog application so regeneration can clean the content folder without deleting the site or its workflows.

## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.EventCatalogPlugin --help
```

To regenerate the options table in this README:

```shell
jbang zw -p EventCatalogPlugin -h markdown
```
