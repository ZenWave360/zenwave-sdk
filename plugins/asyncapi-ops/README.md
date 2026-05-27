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
| `avroImports`    | Avro schema files or folders available while bundling owned message schemas. Supports local files/folders, `classpath:` files/folders and `https://` files.                | List     | `[]`             |                               |
| `authentication` | Authentication configuration values for fetching remote resources.                                                                                                           | List     | `[]`             |                               |
| `server`         | Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Used to merge `x-env-server-overrides`/`env-server-overrides` from channel and error-topic bindings. | String   | `null`           |                               |
| `templates`      | Templates to use for code generation.                                                                                                                                        | String   | `TerraformKafka` | TerraformKafka, TerraformConfluent, TerraformConfluentHybrid, FQ Class Name |
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

Schema files are generated inside the Terraform module and referenced with `${path.module}`. Each bundled file contains a single fully inlined Avro schema JSON object, built from the owned message schema plus only the required types found in `avroImports`.

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

## AsyncAPI extensions used

### Channel bindings: topic configuration

Channel bindings follow the standard [AsyncAPI Kafka binding](https://github.com/asyncapi/bindings/blob/master/kafka/README.md) with one addition: `x-env-server-overrides` for per-environment tuning. The generator also accepts `env-server-overrides` without the `x-` prefix.

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

ACLs are derived from `x-principal` on operation bindings (`send` → Write, `receive` → Read).

Retry and DLQ topics are provisioned from the `x-error-topics` extension on `receive` operations. The generator also accepts `error-topics` without the `x-` prefix.
The consumer group id is read from `x-groupId` (ZenWave extension, plain string). If `groupId` is present it takes precedence — note that in standard AsyncAPI Kafka bindings `groupId` is a schema object, not a plain string; using it as a plain string is a ZenWave-specific interpretation.

```yaml
operations:
  doReserveStockCommand:
    action: receive
    channel:
      $ref: '#/channels/reserve-stock-command'
    bindings:
      kafka:
        x-principal: "merchandising.inventory.inventory-adjustment"
        x-groupId: "merchandising.inventory.inventory-adjustment"
        x-error-topics:
          addressTemplate: "${groupId}.__.${channel.address}.${suffix}"
          retryTopics: 3
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
| `${suffix}` | `retry-0` … `retry-N`, `dlq` |

In this example we use `.__.` as a separator between the consumer group and original topic unambiguously recoverable from the address.

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

## Topic Configuration Defaults

Kafka topic settings: partitions, replication factor, and topic configuration, can be defined at three levels. This section explains how the generator resolves them and what ends up in the generated Terraform.

The precedence order is:

**AsyncAPI value → Terraform variable → provider or broker default**

1. An explicit value in the AsyncAPI spec is always used as-is.
2. A per-environment override from `x-env-server-overrides` / `env-server-overrides` is applied on top of the AsyncAPI value before rendering.
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
