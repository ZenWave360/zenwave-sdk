## What's Changed

- Merged release **2.6.0** by @ivangsa
  https://github.com/ZenWave360/zenwave-sdk/pull/106

## What's New

### AsyncAPI Ops Generator (`asyncapi-ops`)

Generate Terraform HCL to provision Kafka infrastructure directly from your AsyncAPI spec. One run per service produces topics, Schema Registry subjects, ACLs, and retry/DLQ queues as a self-contained Terraform module.

Three provider templates are supported: `TerraformKafka` for OSS Kafka (`Mongey/kafka`), `TerraformConfluent` for Confluent Cloud, and `TerraformConfluentHybrid` for mixed environments. Remote specs are supported with configurable authentication.

Topic ownership is derived automatically from how channels are declared: inline channels are provisioned, `$ref` channels contribute ACLs only. Per-environment overrides, reusable error topic presets, and a layered defaulting contract (AsyncAPI → Terraform variables → provider default) keep specs clean and platform defaults centralized.

This is a beta plugin: it works and is based on real infrastructure automation experience, but expect the extension model and generated output to evolve.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.0...v2.6.0
