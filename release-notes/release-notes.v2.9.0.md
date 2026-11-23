## What's Changed

Release **2.9.0** of ZenWave SDK, built on `dsl-kotlin` 1.11.0 and `architecture-manifest` 0.9.6.

## What's New

### Rules in ZDL

- ZDL 1.11 adds a `rules` block per aggregate and `@rules(...)` on service methods. Generated services read `@rules` and `@policy`, resolving `$.rules` first and then `$.policies`.
- A rule is found by its bare name or qualified by its aggregate: `@rules(Order.limit)` and `@rules(limit)` both find `rules (Order) { limit }`.
- `policies` and `@policy` keep working as deprecated aliases.

## Fixes

- ZDL problems with severity WARNING are logged as warnings, not errors (e.g. the deprecated `policies` keyword).
- Generated Java and Kotlin code compiles for more ZDL models; `in`/`out` are handled as reserved words in the hexagonal layout.
- `@JsonManagedReference` is emitted only for bidirectional JPA relationships.
- JPA `TestDataLoader` reads fixtures by entity name.
- Kotlin generated tests populate nested inputs and DTOs, and call the ZDL method name.
- `zdl-to-openapi` emits `minimum`/`maximum` from `min`/`max` validations.
- `zdl-to-asyncapi-client` generates empty `channels` and `operations` objects.
- `event-catalog`: the manifest's service version wins over the spec's `info.version`.
