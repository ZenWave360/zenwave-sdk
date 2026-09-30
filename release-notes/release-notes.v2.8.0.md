## What's Changed

Draft release notes for the upcoming **2.8.0** release.

Adds Kafka Streams internal-topic authorization to the AsyncAPI Ops (Terraform) generator.

## What's New

### `Lint` plugin — CI-friendly ZDL validation

Linting is a regular plugin, for terminals, agents, and CI (one ZDL file per run; see `jbang zw -p Lint -h`):

```shell
jbang zw -p Lint zdlFile=<file.zdl> [format=text|json|sarif] [output=<file>] [strict=true] [root=<dir>] [linters=<Linter,...>]
```

The report goes to stdout, or to `output=` as UTF-8 (PowerShell 5.1 redirects as UTF-16). Text is `file:line:column: severity ruleId message`; JSON is a versioned envelope (`schemaVersion: "1"`, `summary`, `diagnostics`, `toolFailures`); SARIF is 2.1.0 with `%SRCROOT%`, `rules[]`, `partialFingerprints` and invocation `exitCode`. Positions are 1-based.

`linters` takes an ordered list of `io.zenwave360.sdk.lint.Linter` classes (fully qualified or built-in short names), loaded from the project classpath so custom linters ship in jars added with `--deps`. The default set checks parser problems (including `@lifecycle` / `@transition` states), unresolved `zdl client` / `asyncapi client` references, `@calls` targets, and `@listener` events (plus mixed `@asyncapi`/`@listener` bindings and required-input lineage warnings). Presets are `LintPlugin` subclasses with a different default `linters`. Syntax errors come from `dsl-kotlin` 1.10 as a single problem, and semantic linters are skipped after one, so there are no cascading false errors.

Exit codes are now CLI-wide: `0` success, `1` findings (errors, or warnings with `strict=true`), `2` tool failure. Plugins signal a code by throwing `ZenWaveException(message, exitCode)`; any other exception now exits `2` (previously `1`).

The CLI is now agent-friendly for all plugins: while a plugin runs, `System.out` is redirected to stderr and only deliberate results (the lint report, `ZdlToJson`, template stdout output) reach stdout.

Deprecated: joining several ZDL files into one model (`zdlFiles`) now prints a warning on stderr. Use `zdlFile` and reference other models with `apis { zdl ... }`.

### `x-kafka-streams-applications` — Kafka Streams Internal Topic ACLs

`asyncapi-ops` can now authorize a Kafka Streams application to manage the internal topics it creates for itself at startup — changelog, repartition and join-window store topics — without modelling each one as an AsyncAPI channel and without granting the application open-ended topic-creation rights. Full reference: [plugins/asyncapi-ops/docs/x-kafka-streams.md](../plugins/asyncapi-ops/docs/x-kafka-streams.md).

The extension is declared at the **document root**, because `application.id` belongs to the whole topology rather than to a single `send`/`receive` operation. `kafka-streams-applications` without the `x-` prefix is also accepted.

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    x-transactionalIdPrefix: "merchandising.inventory.inventory-adjustment.streams-app-"
```

- The map key **is** the `application.id`. Since `application.id` and `group.id` are the same value in Kafka Streams, the key must match the effective `groupId`/`x-groupId` of a `receive` operation in the same generator run — same `x-groupId` over `groupId` precedence already used for retry/DLQ topics.
- Generates a `TOPIC`/`PREFIXED` grant on `<application.id>-` with `Create`, `Delete`, `Alter`, `AlterConfigs`, `Describe`, `Read`, `Write`; a `GROUP`/`LITERAL` `Read` grant; and, when `x-transactionalIdPrefix` is present, a `TRANSACTIONAL_ID`/`PREFIXED` `Write` + `Describe` grant.
- **Authorization only.** No `kafka_topic` resources are generated for internal topics: partitions are computed by Kafka Streams from the upstream source topic, and replication factor comes from the application's own `StreamsConfig` or the broker default. Neither is a value this generator could make authoritative.
- A single prefix grant means topology refactors — a renamed `Materialized.as(...)`, a new `KStream.join(...)` — never force a contract change.

#### Closed topic list instead of a prefix

Where governance prefers an auditable list over an open-ended prefix, `topics` replaces the prefix grant with one `LITERAL` grant per name:

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    topics:
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-store-changelog
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-repartition
```

The two modes are mutually exclusive. An explicit `topics: []` generates no `TOPIC` ACL at all while keeping the group and transactional-id grants, for applications whose internal topic management lives entirely outside this extension. Neither mode pre-creates the topics — Kafka Streams still does that itself, and an unlisted internal topic now fails fast at startup with a `TopicAuthorizationException` naming exactly what needs adding.

#### Collision validation

Because the prefix grant includes `Create`, `Alter` and `Delete`, and Kafka matches `PREFIXED` ACLs by plain string prefix with no separator awareness, generation fails when `<application.id>-` overlaps any topic name known to the generator run: owned and external channel addresses, generated retry/DLQ topics, another declared application's namespace (in either direction, so `orders-` and `orders-rebuild-` are rejected), another application's explicit `topics`, or the literal stem of an unresolved `orders-{tenant}`-style address.

The comparison uses `<application.id>-`, the string actually granted, so an `application.id` sharing a dot-separated stem with its own channels is not a collision. Generation also fails on an orphaned entry, a missing `x-principal`, a blank `x-transactionalIdPrefix`, a `topics` entry outside the application namespace, or the same internal topic listed under two applications.

Two notes on scope: the check covers only the specs loaded in one invocation — cross-repository `application.id` clashes need a platform-level naming registry — and the unresolved-address check is deliberately conservative, so declaring `topics` explicitly is the escape hatch when it rejects a spec that would never actually collide.

## Fixes

### Kotlin event emission and MapStruct mapper generation

Kotlin backend application generation now emits events from nullable update and patch results directly
inside the existing `?.also { ... }` block, instead of generating Java `Optional.isPresent()` and
`.get()` calls that produced invalid Kotlin. Event-mapper parameters are also nullable so Kotlin
primitive types such as `Long` use boxed types in kapt stubs, allowing MapStruct to generate mappings
for events emitted from service method parameters such as an entity id.

### Multi-word ACL operations on the Confluent templates

`TerraformConfluent` and `TerraformConfluentHybrid` rendered ACL operations with `{{upper acl.operation}}`, which is correct only for single-word operations: `AlterConfigs` uppercased to `ALTERCONFIGS` instead of the required `ALTER_CONFIGS`. The bug was latent because every operation emitted until now (`Read`, `Write`, `Describe`) is a single word; `ALTER_CONFIGS` is the first multi-word operation to reach a template.

`AclIntent` now carries both spellings — `operation` keeps the `Mongey/kafka` PascalCase form and a derived `confluentOperation` holds the `confluentinc/confluent` `SCREAMING_SNAKE` form — and the two Confluent templates read `confluentOperation` instead of calling `upper`. `TerraformKafka` is unchanged.

Custom `templates=` implementations that render `{{upper acl.operation}}` against the Confluent provider should switch to `{{acl.confluentOperation}}`; existing single-word operations are unaffected either way.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.7.1...v2.8.0
