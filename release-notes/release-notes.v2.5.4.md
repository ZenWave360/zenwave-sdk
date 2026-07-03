## What's Changed

Draft release notes for the upcoming **2.5.4** release.

## What's New

### CLI

- Fixed handling for missing or mistyped plugin names. The CLI now reports a clear `Plugin not found` error instead of falling through to an empty plugin configuration and failing later with a `NullPointerException` while reading the processor chain.
- Added validation for plugin configurations that do not define a processor chain, so malformed custom plugins fail with a targeted error message.

### AsyncAPI JSON Schema to POJO

- Generate POJOs for object schemas declared on selected messages beyond the payload. These are opt-in and off by default, so existing projects keep generating exactly the same classes:
  - `generateMessageKeys` &mdash; message binding keys, e.g. Kafka `bindings.kafka.key` ([#130](https://github.com/ZenWave360/zenwave-sdk/issues/130)).
  - `generateMessageHeaders` &mdash; application headers declared on messages or inherited from message traits ([#120](https://github.com/ZenWave360/zenwave-sdk/issues/120)).
  - `generateBindingHeaders` &mdash; protocol binding header schemas (HTTP/JMS/MQTT).
- Message schemas are now generated in a stable order, and a warning is logged when two schemas would resolve to the same class name instead of one silently overwriting the other.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.3...HEAD
