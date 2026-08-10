# ZDL To AsyncAPI Client Generator Spec

## Goal

Add a generator similar to `ZDLToAsyncAPIPlugin` that reads a ZDL domain model and
generates an `asyncapi-client.yml` file for the AsyncAPI channels consumed by
that domain.

The generated file is a client-side AsyncAPI document. It should not duplicate
provider channel definitions. It should reference provider AsyncAPI documents
with `$ref` entries and define operations for the channels the current domain
consumes.

## Input

The generator reads:

- `apis` block entries that declare external AsyncAPI clients.
- Service methods annotated with `@asyncapi(...)`.
- The current service metadata from `config`.

Example:

```zdl
apis {
    asyncapi client OrdersCheckoutApi "https://../orders-checkout-api/asyncapi.yml"
}

service PaymentsProcessingService for (Payment) {

    @asyncapi(api: OrdersCheckoutApi, channel: OrderCreatedChannel)
    authorizePayment(AuthorizePaymentInput) Payment withEvents PaymentAuthorized

    @asyncapi(channel: PaymentFailedChannel)
    retryPayment(RetryPaymentInput) Payment withEvents PaymentRetried
}
```

## Output

Generate one AsyncAPI 3.0 file per ZDL model:

```text
asyncapi-client.yml
```

The document must include:

```yaml
asyncapi: 3.0.0
id: urn:arcadiaeditions:asyncapi:payments-processing:client
info:
  title: AsyncAPI client for Payments Processing
  version: 1.0.0
  description: Consumed AsyncAPI channels for Payments Processing.
channels: {}
operations: {}
```

## Plugin Configuration

Suggested ZDL plugin configuration:

```zdl
ZDLToAsyncAPIClientPlugin {
    targetFile "./asyncapi-client.yml"
    id "urn:arcadiaeditions:asyncapi:{service-name}:client"
    title "AsyncAPI client for {service-title}"
    version "1.0.0"
}
```

Defaults:

- `targetFile`: `./asyncapi-client.yml`
- `id`: derived from the configured base package or service name.
- `title`: `AsyncAPI client for {config.title}`.
- `version`: `1.0.0`.

## Channel Generation

For every consumed async annotation with an external API:

```zdl
@asyncapi(api: OrdersCheckoutApi, channel: OrderCreatedChannel)
```

Resolve `OrdersCheckoutApi` from the `apis` block:

```zdl
apis {
    asyncapi client OrdersCheckoutApi "https://../orders-checkout-api/asyncapi.yml"
}
```

Generate:

```yaml
channels:
  OrderCreatedChannel:
    $ref: 'https://../orders-checkout-api/asyncapi.yml#/channels/OrderCreatedChannel'
```

For self-consuming annotations without `api` skip the channel generation.

Channel names must match the provider channel names exactly.

## Operation Generation

Generate one receive operation per consumed channel:

```yaml
operations:
  onOrderCreated:
    action: receive
    channel:
      $ref: '#/channels/OrderCreatedChannel'
```

Operation naming rule:

```text
on{ChannelNameWithoutChannelSuffix}
```

Examples:

- `OrderCreatedChannel` -> `onOrderCreated`
- `PaymentFailedChannel` -> `onPaymentFailed`
- `StockReservationConfirmedChannel` -> `onStockReservationConfirmed`

If two channels resolve to the same operation name, fail with a clear error
unless a deterministic disambiguation rule is configured.

## API URL Resolution

The generator does not discover provider contracts by itself. It uses the URL
already declared in the ZDL `apis` block.

Expected supported URL forms:

- Apicurio Registry artifact content URLs.
- HTTP(S) URLs.
- `https://` URLs.

Example Apicurio route:

```text
https://registry.example.com/apis/registry/v3/groups/orders.checkout.orders-checkout/artifacts/orders-checkout-asyncapi/versions/1.0.0/content
```

Example generated reference:

```yaml
channels:
  OrderCreatedChannel:
    $ref: 'https://registry.example.com/apis/registry/v3/groups/orders.checkout.orders-checkout/artifacts/orders-checkout-asyncapi/versions/1.0.0/content#/channels/OrderCreatedChannel'
```

## Example Output

```yaml
asyncapi: 3.0.0
id: urn:arcadiaeditions:asyncapi:payments-processing:client
info:
  title: AsyncAPI client for Payments Processing
  version: 1.0.0
  description: Consumed AsyncAPI channels for Payments Processing.

channels:
  OrderCreatedChannel:
    $ref: 'https://../orders-checkout-api/asyncapi.yml#/channels/OrderCreatedChannel'
  PaymentFailedChannel:
    $ref: 'https://../asyncapi.yml#/channels/PaymentFailedChannel'
  FulfillmentScheduledChannel:
    $ref: 'https://../fulfillment-shipping-api/asyncapi.yml#/channels/FulfillmentScheduledChannel'

operations:
  onOrderCreated:
    action: receive
    channel:
      $ref: '#/channels/OrderCreatedChannel'
  onPaymentFailed:
    action: receive
    channel:
      $ref: '#/channels/PaymentFailedChannel'
  onFulfillmentScheduled:
    action: receive
    channel:
      $ref: '#/channels/FulfillmentScheduledChannel'
```

## Validation

Fail generation when:

- A method references `api: SomeApi`, but `SomeApi` is not declared in `apis`.
- A channel name is missing or empty.
- Two generated operations resolve to the same name.
- An external API URL is malformed.

Warn when:

- A referenced provider file or URL cannot be resolved at generation time.
- The same consumed channel appears on multiple methods.
- A self-consuming channel does not exist in the current service's generated provider AsyncAPI, when that file is available to inspect.

## Non-Goals

- Do not inline provider channel definitions.
- Do not generate producer channels
- Do not infer consumed channels from emitted `withEvents`.
- Do not generate send operations unless a future ZDL annotation explicitly models outbound client commands.
