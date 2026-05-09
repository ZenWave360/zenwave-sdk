# Event Catalog Playground

This diagram proposes topic relationships from the folder structure `<domain>/<subdomain>/<service>/asyncapi.yml` using the convention:

`<domain>.<subdomain>.<service>.<event_name>.<message_type>.<content_type>.<version>`

```mermaid
flowchart LR
  classDef service fill:#0f172a,stroke:#334155,color:#ffffff,stroke-width:1px;
  classDef topic fill:#e2e8f0,stroke:#64748b,color:#0f172a,stroke-width:1px;

  CP[customer-profile<br/>customer-relationship/customer-management]:::service
  LM[loyalty-management<br/>customer-relationship/customer-management]:::service
  IA[inventory-adjustment<br/>merchandising/inventory]:::service
  SR[stock-replenishment<br/>merchandising/inventory]:::service
  PC[price-change<br/>merchandising/pricing]:::service

  CP_CMD[(customer-relationship.customer-management.customer-profile.enroll-loyalty-member.command.avro.v0)]:::topic
  CP_RES[(customer-relationship.customer-management.customer-profile.enroll-loyalty-member.response.avro.v0)]:::topic
  CP_EVT[(customer-relationship.customer-management.customer-profile.customer-profile-updated.event.avro.v0)]:::topic

  LM_CMD[(customer-relationship.customer-management.loyalty-management.refresh-customer-tier.command.avro.v0)]:::topic
  LM_RES[(customer-relationship.customer-management.loyalty-management.refresh-customer-tier.response.avro.v0)]:::topic
  LM_EVT[(customer-relationship.customer-management.loyalty-management.loyalty-tier-updated.event.avro.v0)]:::topic

  IA_CMD[(merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0)]:::topic
  IA_RES[(merchandising.inventory.inventory-adjustment.reserve-stock.response.avro.v0)]:::topic
  IA_EVT[(merchandising.inventory.inventory-adjustment.inventory-adjusted.event.avro.v0)]:::topic

  SR_CMD[(merchandising.inventory.stock-replenishment.replenish-stock.command.avro.v0)]:::topic
  SR_RES[(merchandising.inventory.stock-replenishment.replenish-stock.response.avro.v0)]:::topic
  SR_EVT[(merchandising.inventory.stock-replenishment.stock-replenished.event.avro.v0)]:::topic

  PC_CMD[(merchandising.pricing.price-change.recalculate-price.command.avro.v0)]:::topic
  PC_RES[(merchandising.pricing.price-change.recalculate-price.response.avro.v0)]:::topic
  PC_EVT[(merchandising.pricing.price-change.price-changed.event.avro.v0)]:::topic

  LM -- produces command --> CP_CMD
  CP_CMD -- consumed by owner --> CP
  CP -- produces response --> CP_RES
  CP -- produces event --> CP_EVT
  CP_RES -- consumed by client --> LM
  CP_EVT -- consumed by client --> LM

  CP -- produces command --> LM_CMD
  LM_CMD -- consumed by owner --> LM
  LM -- produces response --> LM_RES
  LM -- produces event --> LM_EVT
  LM_RES -- consumed by client --> CP
  LM_EVT -- consumed by client --> CP

  SR -- produces command --> IA_CMD
  IA_CMD -- consumed by owner --> IA
  IA -- produces response --> IA_RES
  IA -- produces event --> IA_EVT
  IA_RES -- consumed by client --> SR
  IA_EVT -- consumed by client --> SR

  IA -- produces command --> SR_CMD
  SR_CMD -- consumed by owner --> SR
  SR -- produces response --> SR_RES
  SR -- produces event --> SR_EVT
  SR_RES -- consumed by client --> IA
  SR_EVT -- consumed by client --> IA
  SR_EVT -- consumed by client --> PC

  IA -- produces command --> PC_CMD
  PC_CMD -- consumed by owner --> PC
  PC -- produces response --> PC_RES
  PC -- produces event --> PC_EVT
  PC_RES -- consumed by client --> IA
  PC_EVT -- consumed by client --> IA
  PC_EVT -- consumed by client --> SR
```

## Topic Ownership

| Service | Topic | Type | Owner | Producer | Consumer | Clients |
| --- | --- | --- | --- | --- | --- | --- |
| `customer-profile` | `customer-relationship.customer-management.customer-profile.enroll-loyalty-member.command.avro.v0` | `command` | `customer-profile` | `loyalty-management` | `customer-profile` | `loyalty-management` |
| `customer-profile` | `customer-relationship.customer-management.customer-profile.enroll-loyalty-member.response.avro.v0` | `response` | `customer-profile` | `customer-profile` | `loyalty-management` | `loyalty-management` |
| `customer-profile` | `customer-relationship.customer-management.customer-profile.customer-profile-updated.event.avro.v0` | `event` | `customer-profile` | `customer-profile` | `loyalty-management` | `loyalty-management` |
| `loyalty-management` | `customer-relationship.customer-management.loyalty-management.refresh-customer-tier.command.avro.v0` | `command` | `loyalty-management` | `customer-profile` | `loyalty-management` | `customer-profile` |
| `loyalty-management` | `customer-relationship.customer-management.loyalty-management.refresh-customer-tier.response.avro.v0` | `response` | `loyalty-management` | `loyalty-management` | `customer-profile` | `customer-profile` |
| `loyalty-management` | `customer-relationship.customer-management.loyalty-management.loyalty-tier-updated.event.avro.v0` | `event` | `loyalty-management` | `loyalty-management` | `customer-profile` | `customer-profile` |
| `inventory-adjustment` | `merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0` | `command` | `inventory-adjustment` | `stock-replenishment` | `inventory-adjustment` | `stock-replenishment` |
| `inventory-adjustment` | `merchandising.inventory.inventory-adjustment.reserve-stock.response.avro.v0` | `response` | `inventory-adjustment` | `inventory-adjustment` | `stock-replenishment` | `stock-replenishment` |
| `inventory-adjustment` | `merchandising.inventory.inventory-adjustment.inventory-adjusted.event.avro.v0` | `event` | `inventory-adjustment` | `inventory-adjustment` | `stock-replenishment` | `stock-replenishment` |
| `stock-replenishment` | `merchandising.inventory.stock-replenishment.replenish-stock.command.avro.v0` | `command` | `stock-replenishment` | `inventory-adjustment` | `stock-replenishment` | `inventory-adjustment` |
| `stock-replenishment` | `merchandising.inventory.stock-replenishment.replenish-stock.response.avro.v0` | `response` | `stock-replenishment` | `stock-replenishment` | `inventory-adjustment` | `inventory-adjustment` |
| `stock-replenishment` | `merchandising.inventory.stock-replenishment.stock-replenished.event.avro.v0` | `event` | `stock-replenishment` | `stock-replenishment` | `inventory-adjustment`, `price-change` | `inventory-adjustment`, `price-change` |
| `price-change` | `merchandising.pricing.price-change.recalculate-price.command.avro.v0` | `command` | `price-change` | `inventory-adjustment` | `price-change` | `inventory-adjustment` |
| `price-change` | `merchandising.pricing.price-change.recalculate-price.response.avro.v0` | `response` | `price-change` | `price-change` | `inventory-adjustment` | `inventory-adjustment` |
| `price-change` | `merchandising.pricing.price-change.price-changed.event.avro.v0` | `event` | `price-change` | `price-change` | `inventory-adjustment`, `stock-replenishment` | `inventory-adjustment`, `stock-replenishment` |

## Assumptions

- The first three path segments map directly to `<area>.<domain>.<service>`.
- For `command` and `response`, the owner is the service implementing the functionality.
- For `command`, the client is the requesting service that produces the command.
- For `response`, the owner produces the response and the client consumes it.
- For `event`, the owner is the publishing service and the clients are the subscribers.
- `avro` and `v0` are used consistently here as a baseline proposal.
