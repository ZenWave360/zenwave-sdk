# Food Delivery Service with SpringBoot Microservices + AsyncAPI v3

SpringBoot Microservices for a "Food Delivery Service" for a AsyncAPI Technical Talk @sngular


## Online Food Delivery Service

### Getting Started

After cloning the repository, you can build and run the application using:

```shell
docker-compose -f modulith/src/main/docker/docker-compose.yml up -d
mvn clean install -DskipTests
mvn spring-boot:run -f modulith
```
Then use REST APIs to create/update `customers`, `restaurants` and `orders`.

### Bounded Contexts
![Online Food Delivery Service - Bounded Contexts](models/diagrams/BoundedContexts.excalidraw.svg)

### APIs and Models

#### ZenWave ZDL Models:

* [models/customers.zdl](models/customers.zdl)
* [models/orders.zdl](models/orders.zdl)
* [models/restaurants.zdl](models/restaurants.zdl)
* [models/delivery.zdl](models/delivery.zdl)

#### AsyncAPI:

* [customers/src/main/resources/apis/asyncapi.yml](modules/customers/src/main/resources/apis/asyncapi.yml)
* [orders/src/main/resources/apis/asyncapi.yml](modules/orders/src/main/resources/apis/asyncapi.yml)
* [restaurants/src/main/resources/apis/asyncapi.yml](modules/restaurants/src/main/resources/apis/asyncapi.yml)
* [delivery/src/main/resources/apis/asyncapi.yml](modules/delivery/src/main/resources/apis/asyncapi.yml)

#### OpenAPI:

* [customers/src/main/resources/apis/openapi.yml](modules/customers/src/main/resources/apis/openapi.yml)
* [orders/src/main/resources/apis/openapi.yml](modules/orders/src/main/resources/apis/openapi.yml)
* [restaurants/src/main/resources/apis/openapi.yml](modules/restaurants/src/main/resources/apis/openapi.yml)
* [delivery/src/main/resources/apis/openapi.yml](modules/delivery/src/main/resources/apis/openapi.yml)

### Create Order Async Channels

![Online Food Delivery Service - Create Order Async Channels](models/diagrams/CreateOrderChannels.excalidraw.svg)

#### OrderStatus (Happy Path)

```mermaid
stateDiagram-v2
state fork <<fork>>
state join <<join>>
[*] --> RECEIVED
RECEIVED --> fork
fork --> KITCHEN_ACCEPTED
fork --> DELIVERY_ACCEPTED
KITCHEN_ACCEPTED --> join
DELIVERY_ACCEPTED --> join
join --> CONFIRMED
CONFIRMED --> KITCHEN_IN_PROGRESS
KITCHEN_IN_PROGRESS --> KITCHEN_READY
KITCHEN_READY --> KITCHEN_DELIVERED
KITCHEN_DELIVERED --> ON_DELIVERY
ON_DELIVERY --> DELIVERED
DELIVERED --> [*]
```


### Bounded Contexts Entities

![Online Food Delivery Service - Bounded Contexts Entities](models/diagrams/BoundedContexts-Entities.excalidraw.svg)
