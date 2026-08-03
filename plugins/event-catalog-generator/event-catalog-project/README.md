# Welcome to EventCatalog

[EventCatalog](https://www.eventcatalog.dev/) is your architecture catalog for distributed systems. Use it to document, govern, and discover your services, domains, APIs, events, commands, queries, schemas, teams, and flows in one place.

## Start the Catalog

```sh
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to view your catalog.

Using `pnpm` or `yarn`? Run the same scripts with the package manager you chose when creating this project.

## Edit Your Catalog

Your catalog is docs-as-code. Resources live in folders with an `index.mdx` file for frontmatter metadata and Markdown content.

Good places to start:

- `domains/` - bounded contexts, business capabilities, and ownership
- `domains/<domain>/services/` - services, APIs, and message producers or consumers
- `events/`, `commands/`, and `queries/` - the messages flowing through your system
- `teams/` and `users/` - who owns each part of the architecture

Prefer a visual workflow? Run `npm run editor` to open the [EventCatalog Editor](https://www.eventcatalog.dev/docs/editor/overview) and browse, create, or update catalog resources through a GitHub-backed UI.

## Make Your First Change

1. Add or edit a domain, service, or message.
2. Run `npm run dev` and check the generated pages and diagrams.
3. Commit the MDX and schema files to Git.

## Keep It in Sync

Use `npm run generate` with [EventCatalog integrations](https://www.eventcatalog.dev/integrations) to import architecture data from OpenAPI, AsyncAPI, schema registries, EventBridge, GitHub, internal systems, and more.

Run generators locally while you work, or in CI so your catalog stays aligned with the systems it describes.

## Render Schemas from Remote Specifications

`RemoteSpecificationSchema` fetches AsyncAPI 3.x and OpenAPI 3.x documents during a static build. The remote contract remains canonical; rebuild the catalog when it changes.

```mdx
import RemoteSpecificationSchema from '@catalog/components/RemoteSpecificationSchema.astro';

<RemoteSpecificationSchema
  url="https://raw.githubusercontent.com/acme/orders/main/asyncapi.yml"
  message="OrderCancelledMessage"
  title="Order cancelled event"
/>
```

The `message` selector searches both `components.messages` and every `channels.*.messages` map by key, then by the message `name` and `title`. Duplicate matches fail as ambiguous. Generated catalog pages use `channel` and `channelMessage`, preserving the channel message that an operation exposes; `componentMessage` is available for directly authored pages. All message selectors render inline JSON Schema payloads and inline or referenced Avro payloads.

```mdx
<RemoteSpecificationSchema
  url="https://raw.githubusercontent.com/acme/orders/main/asyncapi.yml"
  componentMessage="OrderCancelledMessage"
/>

<RemoteSpecificationSchema
  url="https://raw.githubusercontent.com/acme/orders/main/asyncapi.yml"
  channel="order-events"
  channelMessage="OrderCancelledMessage"
/>
```

OpenAPI component schemas and operation request/response bodies are supported:

```mdx
<RemoteSpecificationSchema url="https://api.example.com/openapi.yml" schema="Order" />

<RemoteSpecificationSchema
  url="https://api.example.com/openapi.yml"
  operationId="createOrder"
  operationTarget="request"
  mediaType="application/json"
/>

<RemoteSpecificationSchema
  url="https://api.example.com/openapi.yml"
  operationId="createOrder"
  operationTarget="response"
  statusCode="201"
  mediaType="application/json"
/>
```

A generic JSONPath selector is also available:

```mdx
<RemoteSpecificationSchema
  url="https://api.example.com/asyncapi.yml"
  jsonPath="$.components.messages.OrderCancelledMessage.payload"
/>
```

URLs and header values interpolate environment variables during the build:

```mdx
<RemoteSpecificationSchema
  url="https://${CONTRACT_HOST}/asyncapi.yml"
  message="OrderCancelledMessage"
  headers={{ Authorization: "Bearer ${EVENTCATALOG_CONTRACT_TOKEN}" }}
/>
```

Headers are forwarded to referenced documents on the root specification's origin. Cross-origin references receive no headers unless their exact origin is allowed:

```mdx
<RemoteSpecificationSchema
  url="https://contracts.example.com/asyncapi.yml"
  message="OrderCancelledMessage"
  headers={{ Authorization: "Bearer ${EVENTCATALOG_CONTRACT_TOKEN}" }}
  forwardHeadersTo={["https://schemas.example.com"]}
/>
```

Missing, unavailable, or invalid remote schemas fail by default. For local development or a deliberately permissive pipeline, use:

```sh
./start-catalog.sh --allow-missing-remote-schemas
./start-catalog.sh --build --allow-missing-remote-schemas
```

In permissive mode, only the affected component becomes a warning panel. The component-level `failOnError={false}` override is also available for exceptional pages. `format="jsonschema"`, `format="avro"`, or `format="raw"` can override automatic format detection. Protobuf is not supported.

## Use AI with Architecture Context

EventCatalog gives AI tools structured context about your domains, services, messages, schemas, owners, and flows.

- Connect Claude, Cursor, VS Code, Windsurf, or another MCP-compatible tool with the [EventCatalog MCP Server](https://www.eventcatalog.dev/docs/development/guides/ai/using-mcp-server).
- Install [EventCatalog Skills](https://github.com/event-catalog/skills) to help agents document services, create domain models, and map business flows:

```sh
npx skills add event-catalog/skills
```

## Learn by Task

- [Create a domain](https://www.eventcatalog.dev/docs/development/guides/domains)
- [Document a service](https://www.eventcatalog.dev/docs/development/guides/services)
- [Add events, commands, and queries](https://www.eventcatalog.dev/docs/development/guides/messages/adding-messages)
- [Use the EventCatalog Editor](https://www.eventcatalog.dev/docs/editor/overview)
- [Generate from integrations](https://www.eventcatalog.dev/integrations)
- [Manage your catalog with the SDK](https://www.eventcatalog.dev/docs/development/sdk)
- [Join the community](https://discord.gg/3rjaZMmrAm)

## Found a Problem?

Open an issue on [GitHub](https://github.com/event-catalog/eventcatalog/issues).
