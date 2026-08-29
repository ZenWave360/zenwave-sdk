## What's Changed

Release notes for the **2.7.1** release.

## What's New

### ZDL Annotation Framework and jMolecules

ZenWave SDK now includes an artifact-aware annotation framework that lets generators add Java
annotations without hard-coding annotation libraries into Handlebars templates. Templates identify
the artifact they generate, while annotators decide which annotations apply to each model element
and generated artifact.

jMolecules is the first built-in implementation of this framework. Generated Java and Kotlin code
can now express DDD building blocks and Hexagonal or Layered Architecture concepts, including
aggregate roots, entities, identities, repositories, domain events, ports, adapters, applications,
and layers. Annotation contribution is idempotent and works across generator chains and custom
templates.

See [ZDL Annotator Framework](../docs/zdl-annotators-framework.md) for the design, extension points,
and jMolecules mapping details.

## Minor Breaking Changes

### Architecture Layout Package Refactoring

The generated package structures for `CleanArchitectureProjectLayout` and
`HexagonalProjectLayout` now follow their respective architecture vocabulary more closely.
This changes generated package names and therefore requires import updates in existing projects
that adopt the new generated output.

`CleanArchitectureProjectLayout` now organizes generated code as:

- `domain` and `domain.event` for the Entities ring.
- `usecase.boundary.input` and `usecase.boundary.output` for use-case boundaries.
- `usecase.interactor` for use-case implementations.
- `adapter.controller`, `adapter.listener`, and `adapter.handler` for input-facing interface adapters.
- `adapter.gateway` for persistence and event-publishing gateways.

`HexagonalProjectLayout` now organizes generated code as:

- `domain` and `domain.event` for the domain model.
- `application.port.in` and `application.port.out` for driving and driven ports.
- `application.service` for driving-port implementations.
- `adapter.in` for driving adapters and `adapter.out` for driven adapters.

Package segments for these layouts now consistently use the singular forms `dto`, `event`,
`mapper`, `command`, and `service`.

### Clean Hexagonal Core Application Package

`CleanHexagonalProjectLayout` now generates application services and their mappers under
`core.application` instead of `core.implementation`:

- `core.implementation` -> `core.application`
- `core.implementation.mappers` -> `core.application.mappers`

This is a minor breaking change for both `CleanHexagonalProjectLayout` and
`DefaultProjectLayout`, which extends it. Existing projects should move customized generated-once
classes, update imports, architecture rules, AOP pointcuts, and any custom template references to
the new package before regenerating.

### Layout-Aware Infrastructure Documentation

Generated Java and Kotlin infrastructure package documentation now links to the configured
outbound package instead of assuming `core.outbound`, so it remains correct for every project
layout.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.7.0...v2.7.1
