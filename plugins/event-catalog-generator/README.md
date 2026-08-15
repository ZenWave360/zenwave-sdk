# Event Catalog Generator

![lifecycle: alpha](https://img.shields.io/badge/lifecycle-alpha-orange)

> Alpha lifecycle: Work in progress. The goal is production-ready, but expect breaking changes and rough edges. Feedback welcome.

Generates Event Catalog from a mix or sources: Markdown, AsyncAPI/Avro, OpenAPI, ZenWave Domain Language (ZDL), ZenWave Flow Language (ZFL)

## Page templates

Frontmatter is built from typed Java records and serialized as YAML. The MDX body for each
EventCatalog resource is rendered from a Handlebars template:

- `domain.mdx.hbs`
- `subdomain.mdx.hbs`
- `service.mdx.hbs`
- `channel.mdx.hbs`
- `event.mdx.hbs`
- `command.mdx.hbs`
- `query.mdx.hbs`
- `entity.mdx.hbs`
- `flow.mdx.hbs`

Built-in templates live below
`io/zenwave360/sdk/plugins/EventCatalogGenerator`. As with the other ZenWave generators, copy
the same relative path below `.zenwave/templates` to override an individual template without
changing the plugin.

## ZFL business flows

Domain-level `zfl` artifacts are converted to EventCatalog flows below
`domains/{domain}/flows/{flow}/index.mdx`. The generator uses `manifest-graph` to resolve
each ZFL `@zdl(...)` reference through the master manifest and links every participating
service back to the generated flow.

ZFL starts, actors, timers, operations, emitted events, handler outcomes, loops, and end
outcomes become EventCatalog flow steps and connections. Published command/query bindings use
native EventCatalog message steps; operations without a published invocation contract remain
visible as documented internal command nodes.

### Logical operations and transport bindings

The generator treats a ZDL operation as the stable semantic identity. OpenAPI operations and
AsyncAPI command channels are invocation bindings of that operation, not replacement identities.

- OpenAPI `GET` and `HEAD` operations become query candidates.
- Other OpenAPI operations become command candidates.
- AsyncAPI command channels remain native EventCatalog commands.
- Compatible REST and AsyncAPI command bindings are reconciled into one command resource.
- Event-triggered or otherwise unbound operations remain internal by default. They are still shown
  in ZFL flows as blue command nodes, with the ZFL documentation and a link to their owning service.
- Set `publishInternalOperations=true` to publish those internal operations as synthesized command
  or query pages as well.

When a published command/query binding exists, a flow uses EventCatalog's native message step for
click-through and versioning. Otherwise the custom blue operation node links to its owning service.
Resource reconciliation and ZFL projection use graph identities exclusively; similarly named
AsyncAPI, OpenAPI, ZDL, or ZFL elements are never merged without a validated `BINDS_TO` edge.

### Breaking command ID migration

Modeled command and query IDs are transport-independent and derive from the logical ZDL operation:
`{service-id}.{operation-slug}`. An AsyncAPI channel may therefore keep its existing channel ID while
the command page changes once from `{service-id}.{channel-slug}` to the logical-operation ID. Existing
links to the old command ID are not redirected; update downstream EventCatalog references during the
dependency rollout.
