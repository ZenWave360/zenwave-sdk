# Backend Application Kotlin Templates

Generates a full backend application using the provided 'layout' property, but using Kotlin instead of Java.

See https://www.zenwave360.io/posts/DDD-In-Practice_From_DSL_to_Complete_Spring-Boot_Kotlin/

## Options

Configured through `BackendApplicationDefaultPlugin` with `templates` pointing at these Kotlin
templates, so it accepts the same options — see
[the backend application plugin](../../backend-application-default/README.md).

`useJSpecify` and `useJMolecules` are supported here too: the annotations are computed once in
`ZDLProcessor` and rendered by the Kotlin templates, including a Kotlin
`JMoleculesArchitectureTest`.
