# ZDL To AsyncAPI Generator
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

Generate AsyncAPI definition from ZDL Services and Events:

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin \
    specFile=src/main/resources/model/orders-model.zdl \
    idType=integer \
    idTypeFormat=int64 \
    targetFile=src/main/resources/model/asyncapi.yml
```

For instance the following ZDL model will generate:

```zdl
service OrdersService for (CustomerOrder) {
    // only emited events will be included in the asyncapi definition
    updateOrder(id, CustomerOrderInput) CustomerOrder withEvents OrderStatusUpdated
}

@asyncapi({channel: "OrderUpdatesChannel", topic: "orders.order_updates"})
event OrderStatusUpdated {
    id String
    dateTime Instant required
    status OrderStatus required
    previousStatus OrderStatus
}
```
- An `schema` named `OrderStatusUpdated` with a `payload` containing the `id`, `dateTime`, `status` and `previousStatus` fields.
- A `message` named `OrderStatusUpdatedMessage` pointing to `OrderStatusUpdated` schema.
- An a `Channel` named `OrderUpdatesChannel` containing a reference to the `OrderStatusUpdatedMessage` message.
- It also will generate an `Operation` named `onOrderStatusUpdated` with and action `send`to the `OrderUpdatesChannel` channel.

This is as a compact format as it can get!! Saving you a lot of typing and giving you very concise representation of your events.

## Options

| **Option**                  | **Description**                                                                                       | **Type**            | **Default**             | **Values**   |
|-----------------------------|-------------------------------------------------------------------------------------------------------|---------------------|-------------------------|--------------|
| `specFile`                  | Spec file to parse                                                                                    | String              |                         |              |
| `targetFolder`              | Target folder to generate code to. If left empty, it will print to stdout.                            | File                |                         |              |
| `targetFile`                | Target file                                                                                           | String              | asyncapi.yml            |              |
| `asyncapiVersion`           | Target AsyncAPI version.                                                                              | AsyncapiVersionType | v3                      | v2, v3       |
| `schemaFormat`              | Schema format for messages' payload                                                                   | SchemaFormat        | schema                  | schema, avro |
| `idType`                    | JsonSchema type for id fields and parameters.                                                         | String              | string                  |              |
| `idTypeFormat`              | JsonSchema type format for id fields and parameters.                                                  | String              |                         |              |
| `basePackage`               | Java Models package name                                                                              | String              | io.example.domain.model |              |
| `avroPackage`               | Package name for generated Avro Schemas (.avsc)                                                       | String              | io.example.domain.model |              |
| `zdlBusinessEntityProperty` | Extension property referencing original zdl entity in components schemas (default: x-business-entity) | String              | x-business-entity       |              |
| `continueOnZdlError`        | Continue even when ZDL contains fatal errors                                                          | boolean             | true                    |              |



## Getting Help

```shell
jbang zw -p io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin --help
```
