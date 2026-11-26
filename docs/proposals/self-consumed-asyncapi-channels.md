# Proposal: self-consumed `@asyncapi` event channels

Status: proposal (working tree, uncommitted). Plugin: `plugins/zdl-to-asyncapi` (`ZDLToAsyncAPIGenerator`).

## Problem

A service method annotated `@asyncapi(channel: "X")` with no `api:`, where `X` is the channel of an event the
same model publishes (`@asyncapi({ channel: "X", topic: "..." }) event E`), means the service privately consumes
its own event (retry loop, saga step). Example: `payments-processing-api/domain-model.zdl`:

```zdl
@asyncapi(channel: "payment-retried-event-v1")
reauthorizePayment(ReauthorizePaymentInput) Payment? withEvents [PaymentAuthorized | PaymentDeclined | PaymentFailed]
@asyncapi(channel: "payment-failed-event-v1")
retryPayment(RetryPaymentInput) Payment? withEvents [PaymentRetried | PaymentRetryExhausted]
@asyncapi(api: OrdersCheckoutApi, channel: "order-confirmation-rejected-event-v1")
@asyncapi(api: FulfillmentShippingApi, channel: "fulfillment-failed-event-v1")
@asyncapi(channel: "payment-capture-failed-event-v1")
voidPayment(VoidPaymentInput) Payment? withEvents [PaymentVoided | PaymentVoidFailed]
```

The generator treated every provider-side `@asyncapi` as an inbound COMMAND channel:

- `buildMethodCommand` (HEAD L219-245) adds `<Input>Command` (avro `<Input>.avsc`) to the channel; `buildEventChannel`
  (HEAD L203-219, `getOrDefault("messages")` L213) then merges the event message into the same channel.
  Result: `payment-retried-event-v1` carries `ReauthorizePaymentInputCommand` + `PaymentRetriedResponse`.
- Bindings were read from `$.options.asyncapi` (HEAD L113-117), i.e. only one occurrence of the repeatable
  annotation (the last). It worked for `voidPayment` only because the no-api occurrence happens to be last.
- `filterSchemasToInclude(model, methodsWithCommands)` (HEAD L152) emitted `ReauthorizePaymentInput.avsc`,
  `RetryPaymentInput.avsc`, `VoidPaymentInput.avsc` though nothing ever publishes them.

Consequences:

1. Two Avro schemas on one topic: with TopicNameStrategy (`<topic>-value`) `AsyncAPIOpsIntent.addSchema`
   (plugins/asyncapi-ops, L72-80) fails: `Schema Registry subject 'payments-processing.payment-retried.event.avro.v1-value'
   resolves to conflicting schema definitions`.
2. Wrong contract: the topic only carries the event.
3. `AsyncAPIGenerator` (provider) generates two overloads per consumer interface, e.g.
   `IPaymentRetriedEventV1ConsumerService.doReauthorizePayment(ReauthorizePaymentInput, ...)` and
   `doReauthorizePayment(PaymentRetried, ...)`; the hand-written adapters must implement the dead one.

## Decided semantics

`@asyncapi(channel: X)` on a method, no `api:` (or a `provider` api), where `X` is declared by an event of this
model => **self-consumed event channel**:

- the channel keeps only its event message(s) (all events declared on `X`; with `withEvents [A | B]` on the same
  channel both are there and the receive operation consumes both: AsyncAPI v3 operations without `messages`
  default to all channel messages);
- a `receive` operation is added on `X`, no `<Input>Command` message, no `<Input>` schema;
- the operation keeps the name `do<Method>` (or `operationId:` when given). `on<Event>` is not an option: it is
  already the `send` operationId of the event (`onPaymentRetried`), and AsyncAPI operation keys must be unique.
  `do<Method>` also keeps the generated consumer method name (`doReauthorizePayment`) stable.
- no visibility extension is added. Channel visibility is an org overlay concern (`x-visibility: global` from
  api-contract-commons `asyncapi-overlay.yml`, not read by any SDK plugin); operations are always owned by the
  service, and AsyncAPIOps needs the receive op as-is (READ ACL + consumer group for the service principal).
  If a marker is later wanted (EventCatalog, docs), `x-zenwave-self-consumed: true` on the operation is the
  suggested name.
- Channel with no own event => unchanged (inbound command channel).
- Every `@asyncapi` occurrence is now a binding (`ZDLFindUtils.methodOptionOccurrences`), same as
  `ZDLToAsyncAPIClientGenerator` and `BackendApplicationAsyncApiAdaptersGenerator` already do.
- Message names unchanged (events of provider-bound methods stay `*Response`), to keep the diff minimal.
  Arguably they should be `*Event`; left as follow-up.
- AsyncAPI v2 output: self-consumed bindings no longer add a command message; no extra operation (legacy).

## Alternatives

| | `@asyncapi(channel: X)` self-consumed (this fix) | `@listener(event: E)` |
|---|---|---|
| Transport | Kafka topic of the event (at-least-once, survives restarts, backoff via retry topics) | in-process Spring Modulith `@ApplicationModuleListener` |
| Works with `@asyncapi` events | yes | no: `@asyncapi` events are published through the generated producer, not `EventPublisher` (`ServiceEventPlanner.needsEventBus`, `ZDLProjectGenerator` L73/146); `ZdlListenerLinter` warns `zdl.mixed-event-binding` |
| Contract | receive op on own channel; AsyncAPIOps grants READ + group | none |
| Fit | microservices (payments-processing-api) | modulith (`arcadia-editions-modulith-api/payments-processing-domain-model.zdl` uses `@listener(event: PaymentRetried)` with events not `@asyncapi`) |

Recommendation: keep both, they are not equivalent. `@listener` = in-process (modulith, non-`@asyncapi` events);
`@asyncapi(channel)` without `api` on an own event channel = self-consumption over the broker. The fix only makes
the second produce a correct contract. No change to `ZDLListenerUtils`.

## Other generators (checked)

- `ZDLToAsyncAPIClientGenerator`: only occurrences whose `api` is a `client` asyncapi api; no-api occurrences are
  ignored (`findConsumedChannels`). `asyncapi-client.yml` of payments has no own channels. No change.
- `backend-application-default` `BackendApplicationAsyncApiAdaptersGenerator`: only reads asyncapi apis declared in
  `apis {}`; the service's own contract is not one, so the own-channel adapters
  (`payments-processing-service-java/.../adapters/events/*ConsumerService.java`) are hand-written. No change. If own
  contracts are later added there, `findServiceMethods` matches by channel, so `doReauthorizePayment` on
  `payment-retried-event-v1` maps to `reauthorizePayment`, and the single message `PaymentRetried` -> `ReauthorizePaymentInput`
  would become a `GENERATED` MapStruct mapping (single message, single bean input).
- EventCatalog: channel kind is classified by name (`*event*` -> EVENT, architecture-manifest `classifyMessage`), so the
  channel remains an event with `_sends` + `_receives` for the service. Fewer messages, no `*Command` entries.
- AsyncAPIOps: one schema per subject; the conflict disappears.

## Verification (payments-processing-api, local build)

Generated into a temp dir with HEAD jar (`before`) and `target/classes` (`after`), same options as the ZDL
`ZDLToAsyncAPIPlugin` block; `before` equals the committed `asyncapi.yml` (except `info.version`).

```diff
   payment-retried-event-v1:
     address: payments-processing.payment-retried.event.avro.v1
     messages:
-      ReauthorizePaymentInputCommand:
-        $ref: "#/components/messages/ReauthorizePaymentInputCommand"
       PaymentRetriedResponse:
   payment-failed-event-v1:
     messages:
-      RetryPaymentInputCommand:
-        $ref: "#/components/messages/RetryPaymentInputCommand"
       PaymentFailedResponse:
   payment-capture-failed-event-v1:
     messages:
-      VoidPaymentInputCommand:
-        $ref: "#/components/messages/VoidPaymentInputCommand"
       PaymentCaptureFailedEvent:
 components.messages:
-    ReauthorizePaymentInputCommand / RetryPaymentInputCommand / VoidPaymentInputCommand
 avro/:
-    ReauthorizePaymentInput.avsc, RetryPaymentInput.avsc, VoidPaymentInput.avsc
```

Operations unchanged (`doReauthorizePayment`, `doRetryPayment`, `doVoidPayment` receive; `on*` send).

`jbang zw -p AsyncAPIOpsGeneratorPlugin apiFiles=asyncapi.yml templates=TerraformConfluent serviceAccountMode=managed server=staging`:
before: `IllegalArgumentException ... conflicting schema definitions`; after: `acls.tf schemas.tf topics.tf versions.tf`
generated, one subject per topic, READ+WRITE ACLs for `payments_processing` on the self-consumed topics.

`AsyncAPIGenerator` (provider, SpringKafka) on the new contract: one method per consumer interface:
`doReauthorizePayment(PaymentRetried, PaymentRetriedHeaders)`, `doRetryPayment(PaymentFailed, ...)`,
`doVoidPayment(PaymentCaptureFailed, ...)`.

## Impact on generated service code

Breaking for implementors of the own-channel consumer interfaces: the `(<Input> payload, <Input>Headers headers)`
overload and its `<Input>Headers` class disappear; `@Override` on them fails to compile. Avro classes
`events.avro.ReauthorizePaymentInput|RetryPaymentInput|VoidPaymentInput` are no longer generated.
The topic payload is unchanged (only the event was ever published), so no runtime/data migration.

## Migration (payments-processing)

1. SDK: release this fix (2.10.x).
2. `payments-processing-api`: regenerate `asyncapi.yml` + `avro/` (removes 3 `*InputCommand` messages and 3 `.avsc`),
   release asyncapi-all/service-all `0.2.2`.
3. `payments-processing-service-java`: bump `payments-processing-api.version` to `0.2.2` (pom + `@import` in
   `zenwave-scripts.zw`); in `PaymentRetriedConsumerService`, `PaymentFailedConsumerService`,
   `PaymentCaptureFailedConsumerService` delete the `do<Method>(<Input> payload, <Input>Headers headers)` overloads and
   the `as<Input>(events.avro.<Input>)` mapper methods; keep event -> input mapping (`PaymentRetried -> ReauthorizePaymentInput`,
   etc.). Update the matching `*ConsumerServiceTest`s.
4. Run AsyncAPIOps provisioning (TopicNameStrategy now valid).
