# AsyncAPI to Terraform Generator

![lifecycle: lab](https://img.shields.io/badge/lifecycle-lab-blueviolet)

> Lab lifecycle: A proof of concept. Built to showcase what's possible, not for production use. Fork it, learn from it, build on it.

Generates Terraform HCL to provision Kafka platform resources: topics, Schema Registry subjects, and ACLs from AsyncAPI specs.

## Command line usage

Single provider spec:
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  avroImports=classpath:shared-avro/avro \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

Multiple spec together for a single service (i.e.: provider + client):
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFiles=asyncapi.yml,asyncapi-client.yml \
  avroImports=schemas/avro1.avsc,schemas/avro2.avsc \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

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

## Configuration options

| **Option**       | **Description**                                                                                                                                                              | **Type** | **Default**      | **Values**                    |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------|-------------------------------|
| `apiFile`        | AsyncAPI Specification File                                                                                                                                                  | URI      | `null`           |                               |
| `apiFiles`       | List of AsyncAPI specs. Supported schemas are local files, http/s and classpath resources.                                                                                   | List     | `[]`             |                               |
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
- `asyncapi client.yml` → `asyncapi_client/avro/...`

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

## How ownership works

You can pass multiple AsyncAPI files through the same `apiFiles` parameter. The generator determines ownership automatically:

- **Owned channel**: declared inline with an `address` field → provisioned as `kafka_topic` + `schemaregistry_schema`
- **External channel**: a `$ref` to another service's file → contributes ACLs and error topics only, no `kafka_topic` or schema resource

```yaml
# asyncapi-client.yml — all channels are external ($ref to provider specs)
channels:
  replenish-stock-command:
    $ref: '../stock-replenishment/asyncapi.yml#/channels/replenish-stock-command'
```

This means you run the generator once per service, passing both that service's provider spec and its client spec, and the right resources are generated automatically.

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

## Defaulting contract

This generator follows a strict defaulting contract for Kafka topic settings:

- Keep `topicConfiguration` optional in AsyncAPI.
- Keep `partitions` and `replicas` optional in AsyncAPI.
- Do not force hardcoded defaults such as `partitions = 1` or `replicas = 1` when the spec omits them.
- Allow platform-wide defaults to be supplied through Terraform variables.
- Apply provider or broker defaults only when the target Terraform provider actually supports that fallback.

The primary contract is:

**AsyncAPI > tfvars > provider-specific fallback**

That means:

1. Explicit AsyncAPI value
2. Environment-specific AsyncAPI override from `x-env-server-overrides` / `env-server-overrides`
3. Terraform module default variable
4. Provider-specific fallback behavior

This is the intended separation of responsibilities:

- AsyncAPI defines intentional per-topic behavior.
- Terraform variables define deployment-time platform defaults.
- The target Terraform provider determines what happens when neither contract layer sets a value.

### Why this model

- Requiring every topic to define `topicConfiguration` is too rigid for general-purpose open source usage and makes simple specs unnecessarily verbose.
- Hardcoding `1` for omitted sizing values is the wrong default because it overrides infrastructure policy with a value that is often not appropriate for production.
- Terraform variables let teams standardize defaults in CI/CD pipelines without repeating the same values in every AsyncAPI file.
- Leaving unresolved settings unset preserves compatibility with managed Kafka platforms and existing infrastructure policies.

### Recommended Terraform contract

The generator should emit Terraform variables such as:

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

### Provider-specific behavior

The exact fallback behavior depends on the selected template provider.

#### `TerraformKafka` (`Mongey/kafka`)

- `partitions` is required by the provider.
- If AsyncAPI omits `partitions`, the generator renders `var.default_partitions`.
- If neither AsyncAPI nor `var.default_partitions` provides a value, Terraform fails and the user must set `default_partitions`.
- `replication_factor` is required by the provider, but the provider accepts `-1`.
- If AsyncAPI omits `replicas`, the generator renders `coalesce(var.default_replication_factor, -1)`.
- That means replication follows `AsyncAPI > tfvars > broker default`.
- `config` follows `AsyncAPI > tfvars > broker default`.

#### `TerraformConfluent` and `TerraformConfluentHybrid` (`confluentinc/confluent`)

- `partitions_count` is optional in the provider and the provider applies its own default when the value is effectively unset.
- If AsyncAPI omits `partitions`, the generator renders `var.default_partitions`.
- If `var.default_partitions` is `null`, Terraform treats the argument as unset and the provider default applies.
- Replication factor is not configurable through `confluent_kafka_topic`, so no Terraform variable is generated for it in these templates.
- `config` follows `AsyncAPI > tfvars > provider/broker default`.

### Generated topic rules

- If AsyncAPI provides `partitions`, render that explicit value.
- Otherwise, render `var.default_partitions` for the selected provider template.
- If AsyncAPI provides `replicas`, render that explicit value.
- Otherwise, use provider-specific fallback behavior:
  - `TerraformKafka`: `coalesce(var.default_replication_factor, -1)`
  - `TerraformConfluent*`: not applicable
- If AsyncAPI provides `topicConfiguration`, merge it on top of `var.default_topic_config`.
- If AsyncAPI does not provide `topicConfiguration`, use `var.default_topic_config`.
- If the resulting config map is empty, render `null` so the provider treats the setting as unset.

For config maps, the recommended merge precedence is:

```hcl
merge(var.default_topic_config, asyncapi_topic_config)
```

That allows platform teams to define shared defaults while still letting individual topics override specific keys in AsyncAPI.

## Maven usage

```xml
<plugin>
    <groupId>io.zenwave360.sdk</groupId>
    <artifactId>zenwave-sdk-maven-plugin</artifactId>
    <version>${zenwave.version}</version>
    <executions>
        <execution>
            <id>generate-terraform</id>
            <phase>generate-resources</phase>
            <goals><goal>generate</goal></goals>
            <configuration>
                <generatorName>AsyncAPIOpsGeneratorPlugin</generatorName>
                <inputSpec>asyncapi.yml</inputSpec>
                <configOptions>
                    <apiFiles>asyncapi.yml,asyncapi-client.yml</apiFiles>
                    <avroImports>${project.basedir}/src/main/asyncapi/avro</avroImports>
                    <server>staging</server>
                    <targetFolder>${project.basedir}/terraform</targetFolder>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.zenwave360.sdk.plugins</groupId>
            <artifactId>asyncapi-ops</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

## Getting help

```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin --help
```
