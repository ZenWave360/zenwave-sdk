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

Built-in templates live below
`io/zenwave360/sdk/plugins/EventCatalogGenerator`. As with the other ZenWave generators, copy
the same relative path below `.zenwave/templates` to override an individual template without
changing the plugin.
