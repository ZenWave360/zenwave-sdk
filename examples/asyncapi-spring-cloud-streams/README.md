# ZenWave Example

https://github.com/ZenWave360/zenwave-code-generator/tree/main/examples/spring-boot-mongo-elasticsearch

## ZenWave Code Generator

#### JDL To AsyncAPI

Generate AsyncAPI definition from JDL entities:

- One channel for each entity update events
- Messages and payloads for each entity Create/Update/Delete events (AVRO and AsyncAPI schema)

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIConfiguration \
    specFile=src/main/resources/model/orders-model.jdl \
    targetFile=src/main/resources/model/asyncapi.yml
```

#### JDL To AsyncAPI+AVRO

Generate AsyncAPI definition from JDL entities:

- One channel for each entity update events
- Messages and payloads for each entity Create/Update/Delete events (AVRO and AsyncAPI schema)

```shell
jbang zw -p io.zenwave360.generator.plugins.JDLToAsyncAPIConfiguration \
    specFile=src/main/resources/model/orders-model.jdl \
    schemaFormat=avro \
    avroPackage=io.zenwave360.example.adapters.events.avro \
    targetFile=src/main/resources/model/asyncapi-avro.yml
```


#### Generate sources

```shell
mvn clean generate-sources
```
