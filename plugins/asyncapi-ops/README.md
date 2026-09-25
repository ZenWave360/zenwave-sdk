# AsyncAPI to Terraform Generator

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)
![lifecycle: beta](https://img.shields.io/badge/lifecycle-beta-red)

> Beta lifecycle: Feature-complete enough for early adopters and real testing, but still evolving. Expect changes, validate in your environment, and use with care in production.

Generates Terraform HCL to provision Kafka platform resources: topics, Schema Registry subjects, and ACLs from AsyncAPI specs.

## Command line usage

Single provider spec:
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  apiOverlayFiles=asyncapi-overlay.yml \
  avroImports=classpath:shared-avro/avro \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

Multiple spec together for a single service (i.e.: provider + client):
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFiles=asyncapi.yml,asyncapi-client.yml \
  apiOverlayFiles=asyncapi-overlay.yml \
  avroImports=schemas/avro1.avsc,schemas/avro2.avsc \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

## Overlays

Use `apiOverlayFiles` to patch each input AsyncAPI before dereferencing and `allOf` merge.

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFiles=asyncapi.yml,asyncapi-client.yml \
  apiOverlayFiles=asyncapi-overlay.yml \
  server=staging \
  targetFolder=terraform/out
```

Overlay files are applied in order to every loaded spec. This is intended for local files and file-backed `classpath:` resources.

Remote files with TerraformConfluent provider:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  authentication.key=Authorization \
  authentication.value="Bearer $TOKEN" \
  authentication.type=HEADER \
  authentication.urlPatterns[0]='https://raw.githubusercontent.com/.*' \
  apiFile=https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/asyncapi.yml \
  avroImports=\
https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/Item.avsc,\
https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/ShoppingCart.avsc \
  templates=TerraformConfluent \
  targetFolder=confluent/work
```

Remote files with authentication:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/asyncapi.yml \
  avroImports=\
https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/Item.avsc,\
https://raw.githubusercontent.com/ZenWave360/zenwave-playground/refs/heads/main/examples/asyncapi-shopping-cart/apis/avro/ShoppingCart.avsc \
  templates=TerraformConfluent \
  targetFolder=confluent/work
```

Hybrid setup with Confluent Kafka resources and standalone Schema Registry provider:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  avroImports=classpath:shared-avro/avro \
  server=staging \
  templates=TerraformConfluentHybrid \
  targetFolder=terraform/inventory-adjustment
```

## Testing Setups

This plugin has been tested in the following setups:

- **Confluent Provider**
    - Service repository: [arcadia-editions/catalog-products-api](https://github.com/arcadia-editions/catalog-products-api)
    - Reusable Terraform/pipeline repository: [arcadia-editions/asyncapi-ops-pipelines](https://github.com/arcadia-editions/asyncapi-ops-pipelines)
- **Kafka OSS**
    - End-to-end test: [TestAsyncAPIOpsTerraformKafkaE2E.java](https://github.com/ZenWave360/zenwave-sdk/blob/main/e2e/src/test/java/io/zenwave360/sdk/e2e/TestAsyncAPIOpsTerraformKafkaE2E.java)


## Configuration options

| **Option**       | **Description**                                                                                                                                                              | **Type** | **Default**      | **Values**                    |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------|-------------------------------|
| `apiFile`        | AsyncAPI Specification File                                                                                                                                                  | URI      | `null`           |                               |
| `apiFiles`       | List of AsyncAPI specs. Supported schemas are local files, http/s and classpath resources.                                                                                   | List     | `[]`             |                               |
| `apiOverlayFiles` | Ordered list of API overlay YAML files applied to each loaded spec before dereferencing and `allOf` merge.                                                            | List     | `[]`             |                               |
| `avroImports`    | Additional Avro schema files or folders available while bundling owned message schemas. Sibling `.avsc` files are discovered automatically for local and `classpath:` schemas. Supports local files/folders, `classpath:` files/folders and `https://` files. | List     | `[]`             |                               |
| `authentication` | Authentication configuration values for fetching remote resources.                                                                                                           | List     | `[]`             |                               |
| `server`         | Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Used to merge `x-env-server-overrides`/`env-server-overrides` from channel and error-topic bindings. | String   | `null`           |                               |
| `parameterValues` | Selects which values a channel address parameter expands to. `parameterValues.<name>=v1,v2` restricts every channel using that parameter name to a subset of its declared `enum`; `parameterValues.<channelKey>.<name>=v1` overrides a single channel and is also the only form that can supply values for a parameter with no `enum`. Validated against each channel's own `enum`. | Map      | `{}`             |                               |
| `templates`      | Templates to use for code generation.                                                                                                                                        | String   | `TerraformKafka` | TerraformKafka, TerraformConfluent, TerraformConfluentHybrid, FQ Class Name |
| `serviceAccountMode` | Resolve existing Confluent service accounts by display name, or provision them as managed Terraform resources.                                                         | String   | `existing`       | existing, managed             |
| `targetFolder`   | Output directory for `.tf` files.                                                                                                                                            | File     | `null`           |                               |

## What it generates

One run per service. All files land in `targetFolder`.

| File | Contents |
|------|----------|
| `versions.tf` | Terraform version constraints |
| `topics.tf` | `kafka_topic` resources for owned channels + retry/DLQ topics |
| `schemas.tf` | `schemaregistry_schema` resources for Avro messages on owned channels |
| `acls.tf` | `kafka_acl` resources derived from operation bindings across all specs |
| `<api-name>/.../*.avsc` | Bundled Avro schema files generated per owned message and referenced from `schemas.tf` |

Schema files are generated inside the Terraform module and referenced with `${path.module}`. Each bundled file contains a single fully inlined Avro schema JSON object. For local and `classpath:` roots, types declared in sibling `.avsc` files are discovered automatically; use `avroImports` for additional schemas located elsewhere or for remote schemas.

When multiple AsyncAPI specs are passed together, bundled schemas are namespaced by the sanitized spec basename:

- `asyncapi.yml` → `asyncapi/avro/...`
- `asyncapi_client.yml` → `asyncapi_client/avro/...`

Terraform resource names are derived from the full Kafka topic address (dots and dashes → underscores), guaranteeing global uniqueness in multi-service modules:

```hcl
resource "kafka_topic" "merchandising_inventory_inventory_adjustment_reserve_stock_command_avro_v0" {
  name               = "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0"
  replication_factor = 3
  partitions         = 3
  config = {
    "cleanup.policy" = "delete,compact"
    "retention.ms"   = "604800000"
  }
}
```

## Available Templates: Terraform Provider Targets

The plugin currently supports three Terraform template targets:

- `TerraformKafka`
  - Uses the OSS providers `Mongey/kafka` for topics and ACLs, and `cultureamp/schemaregistry` for schemas.
  - Best suited for Kafka deployments where topic and ACL management goes through the Kafka provider directly.
  - Supports explicit replication factor management.
- `TerraformConfluent`
  - Uses `confluentinc/confluent` for Kafka topics, ACLs, and Schema Registry resources.
  - Best suited for Confluent Cloud setups where Kafka and Schema Registry are managed through the Confluent provider.
  - Replication factor is not configurable through `confluent_kafka_topic`; partition fallback behavior is provider-specific.
- `TerraformConfluentHybrid`
  - Uses `confluentinc/confluent` for Kafka topics and ACLs, and `cultureamp/schemaregistry` for Schema Registry resources.
  - Best suited for environments that manage Kafka through Confluent but keep schema operations on the standalone Schema Registry provider.

Template selection is controlled with the `templates` option:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  templates=TerraformConfluent \
  targetFolder=terraform/out
```

Choose the template based on the Terraform provider contract you need to integrate with, not just on the Kafka platform brand. Topic defaulting behavior differs across providers and is described in the [Topic Configuration Defaults](#topic-configuration-defaults) section.

## How Topic Ownership Works

AsyncAPI has a natural mirror symmetry: the same channel can be modeled from the sender's point of view or the receiver's. This generator uses that symmetry to decide what to provision.

The rule is simple: pass multiple AsyncAPI files through `apiFiles` and the generator figures out ownership automatically.

- **Owned channel** — declared inline with an `address` field → provisions `kafka_topic` + `schemaregistry_schema`
- **External channel** — a `$ref` to another service's spec → contributes ACLs only, no topic or schema resource

```yaml
# asyncapi-client.yml — all channels are external refs
channels:
  replenish-stock-command:
    $ref: '../stock-replenishment/asyncapi.yml#/channels/replenish-stock-command'
```

ACLs follow the operation direction: `send` gets WRITE + DESCRIBE, `receive` gets READ + DESCRIBE. Every operation gets ACLs regardless of ownership.

Run it once per service, passing both the provider spec and the client spec. The right resources come out the other side.

## Channel Address Parameters

Channel addresses can contain `{parameter}` expressions. How the generator resolves them depends on whether the AsyncAPI [Parameter Object](https://www.asyncapi.com/docs/reference/specification/v3.0.0#parameterObject) declares an `enum`.

### Enumerable parameters → one resource set per value

When `parameters.<name>.enum` is present, the spec is the source of truth and no CLI input is required. One topic (plus its Schema Registry subjects, ACLs and retry/DLQ topics) is generated per declared value:

```yaml
channels:
  orderCreated:
    address: 'orders.{region}.created'
    parameters:
      region:
        enum: [eu, us, apac]
        default: eu
```

```hcl
resource "kafka_topic" "orders_eu_created"   { name = "orders.eu.created"   ... }
resource "kafka_topic" "orders_us_created"   { name = "orders.us.created"   ... }
resource "kafka_topic" "orders_apac_created" { name = "orders.apac.created" ... }
```

`default` is not used for selection — it is a runtime hint, not a provisioning instruction. Terraform resource names are derived from the full resolved address, so every expansion is unique.

Several enumerable parameters on one address expand as a cross product: `orders.{region}.{tenant}.shipped` with 3 regions and 2 tenants provisions 6 topics. Address-independent resources are not multiplied — the consumer group and transactional-id ACLs are still emitted once per operation.

### Restricting the expansion per invocation: `parameterValues`

Use `parameterValues` to generate only a subset of the declared `enum` for a given run, following the same dotted CLI convention as `authentication.key`:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  server=staging \
  parameterValues.region=eu,us \
  parameterValues.orderCreated.region=eu \
  targetFolder=terraform/orders
```

- `parameterValues.<name>` — applies to every channel parameter with that name, whether it is inlined per channel or a `$ref` to `components.parameters`
- `parameterValues.<channelKey>.<name>` — applies to that channel only, and overrides the bare form for it

Most specific wins: channel-scoped value → bare value → the channel's full `enum`. Selected values are emitted in `enum` declaration order, so the generated Terraform is stable regardless of the order they were typed.

Validation is **per channel**, never global. Every supplied value must belong to that channel's own `enum`, and two channels using the same parameter name are never assumed to share allowed values:

```
parameterValues.region=us is not valid for channel userSignedUp (allowed: dev, staging)
```

### Unbounded parameters → channel skipped by default

A parameter without an `enum` (`{streetlightId}`, `{userId}`, `{orderId}`) only has values at runtime. By default that channel is not provisionable and is skipped with a warning naming the channel and the parameter:

- **No topic or schema** — Kafka topics need concrete literal names; there is no way to provision "all possible values".
- **No ACLs either.** A `PREFIXED` pattern was considered and rejected: `orders.{regionId}.created` and `orders.{customerId}.cancelled` share the stem `orders.`, so an automatically-derived prefix grant could hand one principal access to a sibling channel's topics. This goes one step further than external channels, which still get ACLs because their topic is provisioned elsewhere — here there is no concrete topic anywhere to grant access to.
- The rest of the run is unaffected; generation does not fail.

### Provisioning an unbounded parameter anyway

Naming the channel explicitly supplies the values the spec does not declare, and the channel is then generated like any other:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  parameterValues.streetlightMeasured.streetlightId=1,2,3 \
  targetFolder=terraform/streetlights
```

**Only the channel-scoped form works here.** A bare `parameterValues.streetlightId=1,2,3` is ignored for parameters without an `enum` — it would reach every channel using that name across every spec in the run, and there is no `enum` to catch a mistake. Naming the channel means you meant that channel.

Two things follow from there being no declared `enum`:

- Supplied values are **not validated** — a typo becomes a real topic. The values are emitted in the order given, since there is no declaration order to fall back to.
- The value set lives in the pipeline invocation, not in the spec, so two pipelines can generate different topic sets from the same file. Where that matters, prefer putting the truth in the spec: convert the parameter to an `enum`, or override the address to a literal value with `apiOverlayFiles`.

## AsyncAPI extensions used

### Channel bindings: topic configuration

Channel bindings follow the standard [AsyncAPI Kafka binding](https://github.com/asyncapi/bindings/blob/master/kafka/README.md) with one addition: `x-env-server-overrides` for per-environment tuning. The generator also accepts `env-server-overrides` without the `x-` prefix. If both are present, `x-env-server-overrides` wins.

This extension was proposed in [AsyncAPI Bindings #292](https://github.com/asyncapi/bindings/issues/292).

```yaml
channels:
  reserve-stock-command:
    bindings:
      kafka:
        partitions: 20
        replicas: 3
        topicConfiguration:
          cleanup.policy: ["delete", "compact"]
          retention.ms: 604800000
        x-env-server-overrides:
          dev:
            partitions: 1
            replicas: 1
          staging:
            partitions: 3
            replicas: 2
```

Pass `server=staging` to the generator and the staging overrides are deep-merged into the base config before rendering. If no server-specific override exists, the base binding values are used as-is.

### Operation bindings: error topics and ACLs

ACLs are derived from operation bindings. `x-principal` wins over `principal`. `send` operations get topic WRITE + DESCRIBE. `receive` operations get topic READ + DESCRIBE and consumer group READ when a group id can be resolved. Every operation principal also gets Schema Registry `DeveloperRead` access for the Avro subjects used by the operation channel.

`x-principal` is the provider-neutral authenticated account or service-account name and must not include the Kafka `User:` prefix or an environment-specific opaque Confluent `sa-...` id:

```yaml
x-principal: sales.orders.checkout
```

- With `TerraformKafka`, the value is the authenticated account name and the template renders `User:sales.orders.checkout`.
- With `TerraformConfluent` and `TerraformConfluentHybrid`, the value must exactly match a Confluent service account's `display_name`. Human Confluent users are out of scope.

Confluent service-account handling is controlled by `serviceAccountMode`:

- `existing` (default) generates one deduplicated `data "confluent_service_account"` lookup per principal. A missing or ambiguous display name fails during Terraform plan/apply.
- `managed` generates one deduplicated `confluent_service_account` resource per principal. This is useful for fresh forks and demos such as Arcadia:

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  templates=TerraformConfluent \
  serviceAccountMode=managed \
  targetFolder=terraform/out
```

Both modes use the resolved service-account id for Confluent Kafka ACLs. `TerraformConfluent` also uses it for Schema Registry role bindings; `TerraformConfluentHybrid` does not generate Confluent role bindings because its Schema Registry is self-hosted. Application API-key provisioning remains a separate concern.

Retry and DLQ topics are provisioned from the `x-error-topics` extension on `receive` operations. The generator also accepts `error-topics` without the `x-` prefix.
The consumer group id is resolved from `x-groupId` first, then standard `groupId`. `x-groupId` is a plain string. Standard `groupId` must be a schema with `enum`, string `const`, or string-array `const`; the first string value is used.

Transactional producers can request a transactional id prefix ACL with `transactional: true` and `x-transactional-id-prefix`. The Confluent template renders this as a `TRANSACTIONAL_ID` ACL with `PREFIXED` pattern and WRITE operation.

```yaml
operations:
  doReserveStockCommand:
    action: receive
    channel:
      $ref: '#/channels/reserve-stock-command'
    bindings:
      kafka:
        x-principal: "merchandising.inventory.inventory-adjustment"
        groupId:
          type: string
          enum: ["merchandising.inventory.inventory-adjustment"]
        x-error-topics:
          addressTemplate: "${groupId}.__.${channel.address}.${suffix}"
          retryTopics: 3
          retrySuffixes: [retry-5s, retry-30s, retry-5m]
          retry:
            partitions: 1
            replicas: 2
            topicConfiguration:
              retention.ms: 259200000   # 3 days
            env-server-overrides:
              dev:
                replicas: 1
                topicConfiguration:
                  retention.ms: 3600000
          dlq:
            partitions: 1
            replicas: 2
            topicConfiguration:
              retention.ms: 2592000000  # 30 days
              cleanup.policy: ["delete"]
```

The `addressTemplate` variables are:

| Variable | Value |
|----------|-------|
| `${groupId}` | The consumer group id |
| `${channel.address}` | The Kafka topic address of the consumed channel |
| `${suffix}` | `retry-0` … `retry-{N-1}` by default, the corresponding `retrySuffixes` value when configured, or `dlq` |

In this example we use `.__.` as a separator between the consumer group and original topic unambiguously recoverable from the address.

`retrySuffixes` optionally gives retry topics explicit suffixes in order. When omitted, `retryTopics: N` keeps generating `retry-0` through `retry-{N-1}`. When present, `retrySuffixes` must be an array of non-blank strings, contain exactly `retryTopics` entries, and contain no duplicates. Invalid values fail generation, even when the `retry` configuration is absent.

If `x-error-topics`/`error-topics` is not present, no retry or DLQ topics are generated. If a retry or DLQ config does not define env overrides for the selected server, the base retry/DLQ config is used.

### Reusable error topic presets

Extract retry and DLQ configurations into named plans so teams reference approved tiers by name. A shared `kafka-bindings.yml` is a good place for these:

```yaml
components:
  x-error-topics:
    retry:
      silver:
        partitions: 1
        replicas: 2
        topicConfiguration:
          retention.ms: 259200000   # 3 days
        env-server-overrides:
          dev:
            replicas: 1
            topicConfiguration:
              retention.ms: 3600000
      gold:
        partitions: 3
        replicas: 3
        topicConfiguration:
          retention.ms: 604800000   # 7 days
    dlq:
      standard:
        partitions: 1
        replicas: 2
        topicConfiguration:
          retention.ms: 2592000000  # 30 days
          cleanup.policy: ["delete"]
      compliance:
        partitions: 1
        replicas: 3
        topicConfiguration:
          retention.ms: 31536000000 # 1 year
          cleanup.policy: ["delete"]
```

Then reference from any operation:

```yaml
x-error-topics:
  addressTemplate: "${groupId}.__.${channel.address}.${suffix}"
  retryTopics: 3
  retry:
    $ref: 'master/kafka-bindings.yml#/components/x-error-topics/retry/silver'
  dlq:
    $ref: 'master/kafka-bindings.yml#/components/x-error-topics/dlq/compliance'
```

### Document root: Kafka Streams internal topics

A Kafka Streams application creates its own internal topics — changelog, repartition and join-window store topics — at startup with its own `AdminClient`, independently of the broker's `auto.create.topics.enable`. `x-kafka-streams-applications` authorizes that, scoped to a namespace the application owns exclusively, without modelling each internal topic as a channel.

> Design and background notes, including the rationale for each modelling decision and what is deliberately *not* modelled: [docs/x-kafka-streams.md](docs/x-kafka-streams.md).

The extension is declared at the **document root**, alongside `channels` and `operations`, because `application.id` belongs to the whole topology rather than to a single `send`/`receive` edge.

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    x-transactionalIdPrefix: "merchandising.inventory.inventory-adjustment.streams-app-"
```

The map key **is** the `application.id`. In Kafka Streams `application.id` and `group.id` are the same value, so the key must match the effective `groupId`/`x-groupId` of a `receive` operation somewhere in the same generator run — the same `x-groupId` over `groupId` precedence used for retry/DLQ topics.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `x-principal` | string | Yes | Service account to authorize. Rendered as `User:<x-principal>`. |
| `x-transactionalIdPrefix` | string | No | Only for `processing.guarantee=exactly_once_v2`. Its value is the `TRANSACTIONAL_ID` prefix to authorize. |
| `topics` | array\<string\> | No | Explicit internal topic names. Replaces the prefix grant with one literal grant per name. |

#### Unprefixed aliases

The unprefixed spellings are **supported public aliases**, not accidental leniency. These names were proposed upstream for the official AsyncAPI Kafka bindings; the `x-` forms are what you use until that proposal is accepted, and the unprefixed forms are accepted now so that specs written against the standardized names work here already — before and after adoption, without a generator change.

| Documented name | Also accepted |
|---|---|
| `x-kafka-streams-applications` (document root) | `kafka-streams-applications` |
| `x-principal` | `principal` |
| `x-transactionalIdPrefix` | `x-transactional-id-prefix`, `transactionalIdPrefix` |
| `topics` | — already unprefixed; it is a field of the extension object, not a binding-level extension, so it takes no `x-` form |

Where both a prefixed and an unprefixed spelling are present, the `x-` form wins — the same precedence `x-principal`/`principal` and `x-groupId`/`groupId` already follow on operation bindings. Prefer the `x-` forms today: they are unambiguously valid AsyncAPI vendor extensions, and this README documents them as the primary spelling for that reason.

Each entry generates:

| Resource | Pattern | Name | Operations | Condition |
|----------|---------|------|------------|-----------|
| `TOPIC` | `PREFIXED` | `<application.id>-` | `Create`, `Delete`, `Alter`, `AlterConfigs`, `Describe`, `Read`, `Write` | Only when `topics` is absent |
| `TOPIC` | `LITERAL` | each entry of `topics` | `Create`, `Delete`, `Alter`, `AlterConfigs`, `Describe`, `Read`, `Write` | Only when `topics` is present |
| `GROUP` | `LITERAL` | `<application.id>` | `Read` | Always |
| `TRANSACTIONAL_ID` | `PREFIXED` | `x-transactionalIdPrefix` | `Write`, `Describe` | Only when `x-transactionalIdPrefix` is present |

The two `TOPIC` rows are mutually exclusive — `topics` is an alternative mode, not an addition on top of the prefix grant. All grants go to `x-principal`.

This is an authorization-only extension. It provisions no `kafka_topic` resources: partitions are computed by Kafka Streams from the upstream source topic's partition count (already declared on `channel.bindings.kafka.partitions`), and replication factor comes from the application's own `StreamsConfig` or the broker default. Neither is a value this generator could make authoritative.

A single `PREFIXED` grant also means a topology refactor — a renamed `Materialized.as(...)`, a new `KStream.join(...)` — never forces a contract change, since internal topic names are implementation details of the topology.

#### Auditing a closed list instead of a prefix

Where governance prefers a closed, auditable list over an open-ended prefix grant, list the names explicitly:

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    topics:
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-store-changelog
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-repartition
```

- **`topics` absent** — default: one `PREFIXED` grant on `<application.id>-`.
- **`topics` present** — one `LITERAL` grant per name, same operation set, and *no* prefix grant. The two modes are mutually exclusive.
- **`topics: []`** — no `TOPIC` ACL at all; the `GROUP` and `TRANSACTIONAL_ID` grants are still generated. Useful when internal topic management is handled entirely outside this extension.

This does not pre-create the topics — Kafka Streams still creates them itself. If a topology change introduces an unlisted internal topic, Kafka Streams fails fast at startup with a `TopicAuthorizationException` naming exactly what needs to be added, rather than surfacing later as a partition-count mismatch.

#### Validations

Generation fails when:

- The `application.id` matches no `receive` operation's effective group id in the run — an orphaned entry.
- `x-principal` is missing or blank.
- `x-transactionalIdPrefix` is present but not a non-blank string.
- A `topics` entry falls outside `<application.id>-`, or the same internal topic is listed under two applications.
- The granted prefix `<application.id>-` overlaps any other topic name known to the generator run (see below).

Kafka matches `PREFIXED` ACLs by plain string prefix with no separator awareness, and the grant includes `Create`, `Alter` and `Delete` — so anything falling inside the prefix can be altered or deleted by the streams principal. The collision check therefore covers **every topic name the generator knows about, not only resolved channel addresses**:

| Checked against | Why |
|---|---|
| Owned and external channel addresses | Ordinary business topics belonging to this or another service |
| Generated retry/DLQ topic names | Equally real topics, provisioned by this same run |
| `<other application.id>-` of every other declared application, in either direction | `orders-` and `orders-rebuild-` overlap: names exist that start with both, so one application's grant would cover the other's changelog topics |
| The explicit `topics` of every other declared application | A listed internal topic must not fall inside a different application's prefix |
| The literal stem of an unresolved channel address, in either direction | `orders-{tenant}` has no concrete address yet, but some expansion could land inside the namespace, so overlap is rejected conservatively |

Two notes on the comparison:

- It uses `<application.id>-`, the string actually granted, not the bare `application.id`. An `application.id` that shares a dot-separated stem with its own channels (`merchandising.inventory.inventory-adjustment` alongside `merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0`) is **not** a collision, since the address continues with `.` rather than `-`.
- An application using explicit `topics` issues `LITERAL` grants only, so it has no prefix to over-match and is exempt from this check — but its namespace and its listed topics are still protected from other applications' prefixes.

The unresolved-address check is conservative and can reject a spec that would in practice never collide; declaring the internal topics explicitly with `topics` is the escape hatch. An address that *begins* with a parameter expression has no literal stem to compare, so that case is logged rather than failing every application in the run.

The collision check is necessarily scoped to the specs loaded in one generator invocation. Detecting the same clash against an `application.id` chosen in an unrelated repository needs a platform-level naming registry, not a provisioning-time check.

## Topic Configuration Defaults

Kafka topic settings: partitions, replication factor, and topic configuration, can be defined at three levels. This section explains how the generator resolves them and what ends up in the generated Terraform.

The precedence order is:

**AsyncAPI value → Terraform variable → provider or broker default**

1. An explicit value in the AsyncAPI spec is always used as-is.
2. A per-environment override from `x-env-server-overrides` / `env-server-overrides` is applied on top of the AsyncAPI value before rendering. If both are present, the `x-` extension wins.
3. If the AsyncAPI spec omits a setting, the generator renders the corresponding Terraform variable (`var.default_partitions`, `var.default_replication_factor`, `var.default_topic_config`).
4. If the Terraform variable is also unset (`null`), the target provider applies its own default, or the Kafka broker applies its cluster-wide default.

This separation keeps individual specs clean. Topics that need specific sizing say so. Topics that rely on platform standards stay silent and inherit from Terraform variables set in the CI/CD pipeline.

### Terraform Variables

The generator emits the following variables in the module so teams can supply platform-wide defaults without touching individual AsyncAPI files:

```hcl
variable "default_partitions" {
  type    = number
  default = null
}

variable "default_replication_factor" {
  type    = number
  default = null
}

variable "default_topic_config" {
  type    = map(string)
  default = {}
}
```

Confluent templates also emit this variable for generated Schema Registry role bindings:

```hcl
variable "schema_registry_crn" {
  type    = string
  default = null
}
```

Set these in a `terraform.tfvars` file or pass them through your CI/CD pipeline. Topics that specify values in AsyncAPI will override these variables for that specific resource.

For topic configuration maps, the generator merges AsyncAPI values on top of the variable:

```hcl
merge(var.default_topic_config, asyncapi_topic_config)
```

This lets platform teams define shared baseline configuration (retention, cleanup policy) while individual topics can still override specific keys in AsyncAPI.

### Provider-Specific Behavior

The exact behavior when a setting is unset depends on the selected template. Choose the template based on the Terraform provider you are integrating with.

#### `TerraformKafka` (`Mongey/kafka`)

- **Partitions**: required by the provider. If AsyncAPI omits `partitions`, the generator renders `var.default_partitions`. If that variable is also `null`, Terraform fails at plan time — you must set `default_partitions`.
- **Replication factor**: required by the provider, but the provider accepts `-1` to delegate to the broker. If AsyncAPI omits `replicas`, the generator renders `coalesce(var.default_replication_factor, -1)`, which falls back to the broker cluster default when the variable is unset.
- **Topic config**: follows `AsyncAPI → var.default_topic_config → broker default`. If the resulting map is empty, the generator renders `null` so the provider treats the setting as unset.
- **ACLs**: broker ACLs are rendered with `kafka_acl` for topic, group, and transactional-id resources. Transactional-id prefixes use `resource_type = "TransactionalID"` and `resource_pattern_type_filter = "Prefixed"`. Schema Registry permissions are not rendered by this template because they are not Kafka broker ACLs.

#### `TerraformConfluent` (`confluentinc/confluent`)

- **Partitions**: optional in the provider. If AsyncAPI omits `partitions`, the generator renders `var.default_partitions`. If that variable is `null`, Terraform treats the argument as unset and the Confluent provider applies its own default.
- **Replication factor**: not configurable through `confluent_kafka_topic`. No variable is generated for it in this template.
- **Topic config**: follows `AsyncAPI → var.default_topic_config → provider default`. Empty maps render as `null`.

#### `TerraformConfluentHybrid` (`confluentinc/confluent` + `cultureamp/schemaregistry`)

Same partitions and replication behavior as `TerraformConfluent`. Schema Registry resources use the standalone `cultureamp/schemaregistry` provider instead of the Confluent-managed one.

### Generated Topic Rules Summary

| Setting | AsyncAPI present | AsyncAPI absent |
|---------|-----------------|-----------------|
| `partitions` | Rendered as explicit value | Rendered as `var.default_partitions` |
| `replicas` (`TerraformKafka`) | Rendered as explicit value | Rendered as `coalesce(var.default_replication_factor, -1)` |
| `replicas` (`TerraformConfluent*`) | Not applicable | Not applicable |
| `topicConfiguration` | Merged on top of `var.default_topic_config` | Rendered as `var.default_topic_config` |
| Empty config map | — | Rendered as `null` |
