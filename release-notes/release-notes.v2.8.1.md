## What's Changed

Draft release notes for the upcoming **2.8.1** release.

## Fixes

### asyncapi-jsonschema2pojo: external `$ref` schemas generate shared top-level types

Schemas referenced from external files (`$ref: './common.yml#/components/schemas/ColorKind'`) are now generated once as top-level classes in the model package and reused by every model that references them, the same way internal `#/components/schemas/...` references already worked. Previously each referencing class got its own inlined copy (for example a nested `enum ColorKind` in every class).

This applies to every external schema reference, objects and enums alike. **Generated class names may change**: an externally referenced object that used to be inlined with a name derived from the property name (for example `address` → `Address`) is now named after the referenced schema (or its `title` when `useTitleAsClassname` is enabled, or its `javaType`).

References are resolved by their absolute URI, so the same relative `$ref` (for example `./kind.yml#/...`) used from files in different folders now points to the right schema. Previously the first match by raw `$ref` string was used, which could give a property the wrong type.

### asyncapi-jsonschema2pojo: class name clashes are detected for referenced schemas

Two different schemas that resolve to the same class name (for example `Status` in `orders.yml` and `Status` in `payments.yml`) no longer silently share one generated class. The first schema owns the top-level class; any other one is generated inline in the class that references it, and a warning names both sources. Set a distinct `javaType` (or `title`) on one of them to get two top-level classes.

Message payloads taken from different schemas of the same external file are now always treated as distinct schemas, and the same external schema referenced through different relative paths is generated only once.

### asyncapi-jsonschema2pojo: whole-file `$ref` schemas generate shared top-level types

Properties that reference a whole schema file (`$ref: './shade-kind.yml'`, no `#/...` fragment) now also generate one shared top-level class, named after the file without its extension (`shade-kind.yml` → `ShadeKind`), unless the schema sets `javaType` or a `title`. **Generated class names may change** for these properties, which used to be inlined with a name derived from the property name.

A whole-file `$ref` used directly as a message payload keeps the class name it had before (derived from the message).
