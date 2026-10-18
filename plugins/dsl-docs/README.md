# DslDocs

Generates multi-page Markdown documentation with Mermaid diagrams from ZDL, ZFL, OpenAPI and AsyncAPI files.

```shell
jbang zw -p DslDocs \
    zdlFiles=orders.zdl,payments.zdl \
    zflFiles=place-order.zfl \
    apiFiles=openapi.yml,asyncapi.yml \
    targetFolder=docs
```

Use `check=true` in CI to compare the generated output without writing it. It exits with code `1` when documentation
is out of date, an input is invalid, or a generated page would overwrite hand-written Markdown.
