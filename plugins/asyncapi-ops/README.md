# AsyncAPI to Terraform Generator

![lifecycle: lab](https://img.shields.io/badge/lifecycle-lab-blueviolet)

> Lab lifecycle: A proof of concept. Built to showcase what's possible, not for production use. Fork it, learn from it, build on it.

Generates Terraform HCL to provision Kafka platform resources: topics, Schema Registry subjects, and ACLs from AsyncAPI specs.

## Command line usage

Single provider spec:
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFile=asyncapi.yml \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

Multiple spec together for a single service (i.e.: provider + client):
```shell
jbang zw -p AsyncAPIOpsGeneratorPlugin \
  apiFiles=asyncapi.yml,asyncapi-client.yml \
  server=staging \
  targetFolder=terraform/inventory-adjustment
```

## Configuration options

| **Option**       | **Description**                                                                                                                                                              | **Type** | **Default**      | **Values**                    |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------|-------------------------------|
| `apiFile`        | AsyncAPI Specification File                                                                                                                                                  | URI      | `null`           |                               |
| `apiFiles`       | List of AsyncAPI specs. Supported schemas are local files, http/s and classpath resources.                                                                                   | List     | `[]`             |                               |
| `authentication` | Authentication configuration values for fetching remote resources.                                                                                                           | List     | `[]`             |                               |
| `server`         | Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Used to merge env-server-overrides from channel and error-topic bindings. | String   | `null`           |                               |
| `templates`      | Templates to use for code generation.                                                                                                                                        | String   | `TerraformKafka` | TerraformKafka, FQ Class Name |
| `targetFolder`   | Output directory for `.tf` files.                                                                                                                                            | File     | `null`           |                               |

## What it generates

One run per service. All files land in `targetFolder`.

| File | Contents |
|------|----------|
| `versions.tf` | Terraform version constraints |
| `topics.tf` | `kafka_topic` resources for owned channels + retry/DLQ topics |
| `schemas.tf` | `schemaregistry_schema` resources for Avro messages on owned channels |
| `acls.tf` | `kafka_acl` resources derived from operation bindings across all specs |

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

Channel bindings follow the standard [AsyncAPI Kafka binding](https://github.com/asyncapi/bindings/blob/master/kafka/README.md) with one addition: `x-env-server-overrides` for per-environment tuning.

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

Pass `server=staging` to the generator and the staging overrides are deep-merged into the base config before rendering.

### Operation bindings: error topics and ACLs

ACLs are derived from `x-principal` on operation bindings (`send` → Write, `receive` → Read).

Retry and DLQ topics are provisioned from the `x-error-topics` extension on `receive` operations.
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
