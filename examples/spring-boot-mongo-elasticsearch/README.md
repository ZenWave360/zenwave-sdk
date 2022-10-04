# ZenWave Example

https://github.com/ZenWave360/zenwave-code-generator/tree/main/examples/spring-boot-mongo-elasticsearch

## ZenWave Code Generator

### Install ZenWave

```shell
jbang alias add -name=zw zw-release@zenwave360/zenwave-code-generator
```

or if you prefer to use the latest **snapshot** versions:

```shell
jbang alias add --name=zw \
    -m=io.zenwave360.generator.Main \
    --repos=mavencentral,snapshots=https://s01.oss.sonatype.org/content/repositories/snapshots \
    --deps=\
org.slf4j:slf4j-simple:1.7.36,\
io.github.zenwave360.zenwave-code-generator.plugins:asyncapi-spring-cloud-streams3:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:asyncapi-jsonschema2pojo:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:openapi-spring-webtestclient:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-backend-application-default:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-to-openapi:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-to-asyncapi:0.7.0-SNAPSHOT,\
io.github.zenwave360.zenwave-code-generator.plugins:jdl-openapi-controllers:0.7.0-SNAPSHOT\
    io.github.zenwave360.zenwave-code-generator:zenwave-code-generator-cli:0.7.0-SNAPSHOT
```

### Generate Backend Application

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration \
    specFile=src/main/resources/model/orders-model.jdl \
    basePackage=io.zenwave360.example \
    persistence=mongodb \
    style=imperative \
    targetFolder=.
```

#### JDL To OpenAPI

Generate OpenAPI definition from JDL entities:

- Component Schemas for entities, plain and paginated lists
- CRUD operations for entities

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToOpenAPIConfiguration \
    specFile=src/main/resources/model/orders-model.jdl \
    targetFile=src/main/resources/model/openapi.yml
```

#### JDL To AsyncAPI

Generate AsyncAPI definition from JDL entities:

- One channel for each entity update events
- Messages and payloads for each entity Create/Update/Delete events (AVRO and AsyncAPI schema)

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIConfiguration \
    specFile=src/main/resources/model/orders-model.jdl \
    targetFile=src/main/resources/model/asyncapi.yml
```


#### SpringMVC Controllers from OpenAPI

Delete generated CRUD Controllers:

```shell
rm -rf src/main/java/io/zenwave360/example/adapters/web
```

Generate new SpringMVC controllers from OpenAPI:

```shell
mvn clean generate-sources
```

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLOpenAPIControllersConfiguration \
    specFile=src/main/resources/model/openapi.yml \
    jdlFile=src/main/resources/model/orders-model.jdl \
    basePackage=io.zenwave360.example \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    targetFolder=.
```

#### Spring WebTestClient

Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification.

```shell
jbang zw -p io.zenwave360.generator.plugins.SpringWebTestClientConfiguration \
    specFile=src/main/resources/model/openapi.yml \
    jdlFile=src/main/resources/model/orders-model.jdl \
    targetFolder=src/test/java \
    controllersPackage=io.zenwave360.example.adapters.web \
    openApiApiPackage=io.zenwave360.example.adapters.web \
    openApiModelPackage=io.zenwave360.example.adapters.web.model \
    openApiModelNameSuffix=DTO \
    groupBy=SERVICE
```
