# `x-kafka-streams-applications` — AsyncAPI Kafka Extension

## Overview

`x-kafka-streams-applications` is an extension that lets a Kafka Streams application
manage its own internal topics (changelog topics, repartition topics, join-window
store topics) without provisioning them as individual AsyncAPI channels and without
granting the application unrestricted topic-creation rights.

Kafka Streams creates these topics itself at startup, using its own internal
`AdminClient` — this happens independently of the broker's `auto.create.topics.enable`
setting. The platform-appropriate control point is not "can this client auto-create
topics" but "what is this client allowed to create, alter, and delete", scoped to a
namespace it owns exclusively.

This extension is intentionally scoped to **authorization**. It does not declare
topics, partitions, or replication factors for internal topics — see
[Why internal topic configuration is not modeled](#why-internal-topic-configuration-is-not-modeled).
Source and output topics of the topology are ordinary AsyncAPI channels/operations and
are unaffected by this extension.

---

## Placement

`x-kafka-streams-applications` is declared at the **document root**, alongside
`channels` and `operations` — not inside an `operation` or a `channel`.

`application.id` is a property of the whole topology, not of a single `send`/`receive`
edge. A topology commonly has several source topics (several `receive` operations)
sharing one `application.id`; attaching this extension to one arbitrarily chosen
operation would force either duplicating it across every `receive` operation of the
same app or picking an owner operation that doesn't actually own the concept.

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    x-transactionalIdPrefix: "merchandising.inventory.inventory-adjustment.streams-app-"

operations:
  processStockEvents:
    action: receive
    channel:
      $ref: '#/channels/stock-movement-event'
    bindings:
      kafka:
        x-principal: "merchandising.inventory.inventory-adjustment"
        x-groupId: "merchandising.inventory.inventory-adjustment.streams-app"
```

The map key (`merchandising.inventory.inventory-adjustment.streams-app`) **is** the
`application.id`. In Kafka Streams, `application.id` and `group.id` are the same value,
so this key is expected to match the effective `groupId`/`x-groupId` of the `receive`
operation(s) that feed the topology — the same resolution precedence (`x-groupId` over
`groupId`) already used for retry/DLQ topics applies here.

---

## Schema

### `x-kafka-streams-applications`

A map keyed by `application.id`.

| Field | Type | Required | Description |
|---|---|---|---|
| `x-principal` | `string` | Yes | Service account to authorize. Rendered as `User:<x-principal>` in generated ACLs. |
| `x-transactionalIdPrefix` | `string` | No | Present only when the application runs with `processing.guarantee=exactly_once_v2` (EOS). Its value is the `TRANSACTIONAL_ID` prefix to authorize. |
| `topics` | `array<string>` | No | Explicit list of pre-computed internal topic names. When present, replaces the `PREFIXED` grant with one `LITERAL` grant per listed topic. See [`topics` mode](#topics-mode-explicit-literal-topic-list). |

---

## Provisioning

Each entry in `x-kafka-streams-applications` generates the following ACLs. Operation
lists are fixed — they are not configurable per entry.

| Resource | Pattern type | Resource name | Operations | Condition |
|---|---|---|---|---|
| `TOPIC` | `PREFIXED` | `<key>-` | `CREATE`, `DELETE`, `ALTER`, `ALTER_CONFIGS`, `DESCRIBE`, `READ`, `WRITE` | Always, unless `topics` is present |
| `TOPIC` | `LITERAL` | each entry of `topics` | `CREATE`, `DELETE`, `ALTER`, `ALTER_CONFIGS`, `DESCRIBE`, `READ`, `WRITE` | Only if `topics` is present |
| `GROUP` | `LITERAL` | `<key>` | `READ` | Always |
| `TRANSACTIONAL_ID` | `PREFIXED` | `x-transactionalIdPrefix` | `WRITE`, `DESCRIBE` | Only if `x-transactionalIdPrefix` is present |

All resource ACLs are granted to `x-principal`. The `TOPIC`/`PREFIXED` and `TOPIC`/`LITERAL` grants are mutually exclusive — `topics` is an alternative mode, not an addition on top of the prefix grant.

### `topics` mode: explicit literal topic list

Some governance teams prefer not to grant an open-ended `PREFIXED` authorization
(which allows creating/altering/deleting any future topic under that prefix) and
instead want to audit a closed list of exact names:

```yaml
x-kafka-streams-applications:
  merchandising.inventory.inventory-adjustment.streams-app:
    x-principal: "merchandising.inventory.inventory-adjustment"
    topics:
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-store-changelog
      - merchandising.inventory.inventory-adjustment.streams-app-by-sku-repartition
```

Semantics:

- **`topics` absent**: default behavior, `PREFIXED` grant on `<key>-`.
- **`topics` present**: one `LITERAL` grant is generated per listed name, with the same
  operation set as the `PREFIXED` mode. No `PREFIXED` grant is generated.
- **`topics: []`** (explicit empty list): no `TOPIC` ACL is generated for this
  application at all. The `GROUP` grant (and `TRANSACTIONAL_ID`, if applicable) is
  still generated. Useful when topic management for this app is handled entirely
  outside this extension.

Kafka Streams still creates the internal topics itself with its own `AdminClient` —
this mode does not pre-create them, it only narrows the ACL to a closed list instead
of an open prefix. This is the same approach observed in production JulieOps repos at
Banco Sabadell (each internal topic listed by hand), with one meaningful difference in
failure mode: JulieOps pre-created the topic itself, so a topology change introducing
a new, unlisted internal topic could surface as a missing topic or a partition-count
mismatch, sometimes caught late. Here, if a new unlisted internal topic appears, Kafka
Streams attempts to create it and fails fast at startup with a
`TopicAuthorizationException` — an explicit failure that points directly at what needs
to be added to the list, instead of a silent mismatch.

### Why `TOPIC`/`PREFIXED` and not one ACL per topic

Kafka Streams derives internal topic names from `application.id` plus DSL-specific
suffixes generated from the topology (state store names, join names, repartition
operators) — e.g. `<application.id>-<store>-changelog`,
`<application.id>-<node>-repartition`. These names are implementation details of the
topology, not part of the service's public contract, and they change whenever the
topology is refactored (a renamed `Materialized.as(...)`, a new `KStream.join(...)`).

Modeling each one as a `channel` couples the AsyncAPI contract to internal
implementation details and forces a contract change on every topology refactor. A
single `PREFIXED` grant scoped to `<application.id>-` lets Kafka Streams create,
alter, and delete only topics inside its own namespace, regardless of how the topology
evolves — without ever widening the grant beyond that one application's own topics.

### Why internal topic configuration is not modeled

There is no field for partitions, replication factor, or `topicConfiguration` on
internal topics, and this is deliberate:

- **Partitions** are computed by Kafka Streams from the partition count of the
  topology's upstream source topic(s) — already declared on the corresponding
  `channel.bindings.kafka.partitions`. They are not a free value an intent file could
  set; Kafka Streams refuses to start if a pre-existing topic doesn't match the
  partition count it computes.
- **Replication factor** comes from the application's own `StreamsConfig`
  (`replication.factor`); when unset, it falls back to the broker's
  `default.replication.factor`. No value here would be authoritative — the generator
  cannot override what the application's own configuration or the cluster's defaults
  produce at topic-creation time.

There is no lever this extension could add that Kafka Streams would actually honor.

---

## Validations

Generation should fail explicitly when:

- **Mismatched consumer group** — the `application.id` (the key in
  `x-kafka-streams-applications`) does not match the effective `groupId`/`x-groupId` of
  any `receive` operation in the processed specs. An entry with no matching operation
  is orphaned and MUST be rejected rather than silently provisioned.
- **Prefix collision** — the granted prefix `<application.id>-` overlaps any other topic
  name known to the generator run. `PREFIXED` ACL matching in Kafka is plain
  string-prefix matching with no separator awareness, and the granted operations include
  `CREATE`, `ALTER` and `DELETE`, so anything falling inside the prefix is destructible
  by the Streams principal. The check covers:
  - owned and external `channel.address` values;
  - generated retry/DLQ topic names, which are equally real topics;
  - the internal topic namespace `<other application.id>-` of every other declared
    application, in **either** direction — `orders-` and `orders-rebuild-` overlap
    because names exist that start with both, so `orders-` would cover another
    application's changelog topics;
  - the explicit `topics` entries of every other declared application;
  - the literal stem of an unresolved (unbounded-parameter) channel address such as
    `orders-{tenant}`, compared conservatively in either direction, since some expansion
    of the parameter could land inside the namespace. An address beginning with a
    parameter expression has no literal stem and says nothing about its concrete names;
    that case is logged rather than failing every application.

  The check compares the string actually granted, not the bare `application.id`: an
  `application.id` that shares a dot-separated stem with its own channels is not a
  collision, because those addresses continue with `.` rather than `-`. Checking the
  bare `application.id` would reject that common naming convention.

  An application using explicit `topics` issues `LITERAL` grants only, so it has no
  prefix to over-match and is not subject to this check — though its namespace and its
  listed topics are still protected *from* other applications' prefixes.

  This check is necessarily scoped to the specs loaded in one generator invocation
  (`apiFiles`); detecting the same collision against `application.id`s chosen in
  unrelated repositories processed by separate pipeline runs is out of scope for this
  extension and for the generator — it requires a platform-level naming registry, not
  a provisioning-time check.
- `x-principal` missing on an entry.
- `x-transactionalIdPrefix` present but empty or not a string.
- **`topics` entry outside the application's namespace** — every name listed in
  `topics` MUST start with `<key>-`. Kafka Streams always prefixes its internal topics
  with `application.id` followed by a separator; a name that doesn't match this
  pattern is almost certainly a transcription error or an attempt to smuggle an
  unrelated grant through this mechanism.
- **Topic listed under more than one application** — the same name MUST NOT appear in
  the `topics` list of two different `x-kafka-streams-applications` entries; each
  internal topic belongs to exactly one application.

---

## Implementation notes (ZenWave AsyncAPIOpsIntentProcessor)

**Implemented.** `AclIntent` was already generalized before this extension landed: it carries
`resourceType`/`kafkaResourceType` and `patternType`/`kafkaPatternType` (defaulting to
`TOPIC`/`LITERAL`), and all three ACL templates render them, so `TOPIC`/`PREFIXED`,
`GROUP` and `TRANSACTIONAL_ID` grants needed no model or template change.

The operation-name casing hazard described below was real. The Confluent templates applied
`{{upper acl.operation}}` to values stored in PascalCase, which is correct only for
single-word operations — `AlterConfigs` uppercases to `ALTERCONFIGS`, not `ALTER_CONFIGS`.
Both provider vocabularies were confirmed against their published resource docs:

| Provider | `resource_type` | `pattern_type` | Operations |
|---|---|---|---|
| `Mongey/kafka` | `Topic`, `Group`, `TransactionalID` | `Literal`, `Prefixed` | `Read`, `Write`, `Create`, `Delete`, `Alter`, `Describe`, `AlterConfigs`, … |
| `confluentinc/confluent` | `TOPIC`, `GROUP`, `TRANSACTIONAL_ID` | `LITERAL`, `PREFIXED` | `READ`, `WRITE`, `CREATE`, `DELETE`, `ALTER`, `DESCRIBE`, `ALTER_CONFIGS`, … |

The two operation vocabularies are related by `snakeCase().toUpperCase()` across every member,
so `AclIntent.operation` keeps the Mongey PascalCase spelling and a derived
`AclIntent.confluentOperation` carries the Confluent spelling. The Confluent templates read
that field instead of calling `upper`; `TerraformKafka/acls.tf.hbs` was left untouched.
`{{upper acl.permissionType}}` is unaffected — `Allow`/`Deny` are single words.

Processing runs in two passes because two validations are cross-spec: the first pass collects
every resolved channel address and every `receive` operation's effective group id across all
loaded specs, and the second validates and emits the streams ACLs. Emitting last also groups
them at the end of the generated `acls.tf`. The group id is resolved above the early returns
in `processOperation`, so a `receive` operation contributing no ACLs (no principal, unbounded
address parameter) still counts as a match for an `x-kafka-streams-applications` entry.

`topics` mode needed no further model changes: the processor branches on whether the key is
present and emits either one `PREFIXED` `AclIntent` or one `LITERAL` `AclIntent` per listed
name. `containsKey` distinguishes an absent `topics` from `topics: []`.

Root-level extensions survive parsing — `AsyncApiProcessor` mutates the model map in place —
so the entry is read straight off the document root. Note that `Model` overrides only
`entrySet()`, so tests mutating the root must go through `Model.model()`; `put` on the `Model`
itself hits `AbstractMap`'s unsupported default.

## Future Considerations

- If adopted into the official AsyncAPI Kafka bindings, the `x-` prefix would be
  dropped from `x-kafka-streams-applications` and `x-transactionalIdPrefix`. **Already
  implemented:** the unprefixed spellings (`kafka-streams-applications`, `principal`,
  `transactionalIdPrefix`) are accepted today as public aliases, so a spec written
  against the standardized names works before and after adoption with no generator
  change. The `x-` form wins when both are present.
- A `templates=TerraformKafka` note: `Mongey/kafka`'s `kafka_acl` resource requires one
  resource per operation, identical to how business-topic ACLs are already expanded
  today — no new expansion mechanism is needed beyond generating more `AclIntent`
  entries.
