# ZenWave SDK

> 👉 ZenWave360 Helps You Create Software Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.zenwave360.sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.zenwave360.sdk/zenwave-sdk)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/ZenWave360/zenwave-sdk?logo=GitHub)](https://github.com/ZenWave360/zenwave-sdk/releases)
![General Availability](https://img.shields.io/badge/lifecycle-GA-green)
[![build](https://github.com/ZenWave360/zenwave-sdk/workflows/Build/badge.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/jacoco.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![branches coverage](https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/badges/branches.svg)](https://github.com/ZenWave360/zenwave-sdk/actions/workflows/build.yml)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

## Domain Driven Design (DDD) and API-First for Event Driven Microservices

ZenWave SDK is a configurable and extensible code generator tool for **Domain Driven Design (DDD)** and **API-First** for **Event Driven Microservices** that can generate code from a mix of different models including:

ZenWave SDK can generate code from a mix of different models including:

- [ZDL Domain Language](https://zenwave360.github.io/docs/event-driven-design/zenwave-domain-language) as **Ubiquitous Language**. You can describe the core of your Bounded Context, as well as how it connects to external systems through different adapters and APIs.
- [AsyncAPI](https://www.asyncapi.com/docs/getting-started/coming-from-openapi): Industry de-facto standard to describe Event-Driven Architectures for Message-based APIs.
- [OpenAPI](https://swagger.io/specification/): Industry standard for Request-Response Architectures with REST APIs.

Using ZenWave Domain Language as **Ubiquitous Language** for modeling and describing Bounded Contexts: aggregates, entities with their relationships, services, value objects, commands and events.

ZenWave Domain Language started as an *extended subset* of [JHipster Domain Language (JDL)](https://www.jhipster.tech/jdl/intro) that let you describe your entities and relationships.

Using ZDL Domain Language as **Ubiquitous Language** for **Data on the Inside** and **API-First** specs like **AsyncAPI** and **OpenAPI** to describe Inter Process Communications (IPC) for **Data on the Outside**.

- **ZenWave Domain Language (ZDL) as Ubiquitous Language:** To describe your domain core domain model.
- **API-First specs like AsyncAPI and OpenAPI:** to describe Inter Process Communications (IPC) between bounded contexts/microservices.
- **ZenWave SDK:** to generate (_a lot of_) infrastructure, functional and testing code from your models and APIs.

ZenWave SDK is designed to be easily extensible and adaptable to your project or your organization needs and likes. You can always [fork an existing, standard or custom plugin](https://github.com/ZenWave360/zenwave-sdk/#forking-an-standard-or-custom-plugin).

You can install the latest release using [jbang](https://www.jbang.dev) running the following command:

```shell
jbang alias add --fresh --name=zw release@zenwave360/zenwave-sdk
```

Please refer to [**ZenWave SDK**](https://github.com/ZenWave360/zenwave-sdk/) for more detailed installation options.

**Note:** Official plugins are designed to generate functional code and tests on top of existing projects. Creating a base project is out of scope, but you can always go to [start.spring.io](http://start.spring.io) or [start.jhipster.tech](https://start.jhipster.tech/), in case your company doesn't already have a project starter or archetype.

## Not (just) a Code Generator

ZenWave SDK is a **Modeling Tool** for Domain Driven Design and API-First in disguise of a _code generator_.

Its purpose is to produce successful software projects by dramatically shortening the feedback loop between the expert domain knowledge and working software and its tests.

![Domain Driven Design Feedback Loop](docs/ZenWave-360-DDD-Feedback-Loop-with-ZW-Products.excalidraw.svg)

In this way all team members: **Domain Experts**, **Product Owners**, **Software Architects**, **Developers** and **Testers** can provide early feedback based on an **Ubiquitous Language (JDL)** and the software and tests generated from that model.

## Why Domain Driven Design?

> "There are three types of developers implementing microservices. Those who use DDD, those who don't realise they do, and those who fail."

**DDD:** is about building software around a domain model that represents the problem we want to solve. Expressed by and Ubiquitous Language that is shared by all team members. It helps understand the problem before thinking of a solution. It connects Domain Experts with Technical Experts building a shared understanding of the problem and the solution.

**ZenWave360:** is about speeding up the feedback loop from idea -> model -> working software and tests.

<div style="text-align: center;" markdown="1">
  <img src="docs/ZenWave360-Design-Code-Loop.excalidraw.svg" alt="Design to Code" />
</div>


## Table of Contents

<!-- TOC -->
- [ZenWave SDK](#zenwave-sdk)
  - [Domain Driven Design (DDD) and API-First for Event Driven Microservices](#domain-driven-design-ddd-and-api-first-for-event-driven-microservices)
  - [Not (just) a Code Generator](#not-just-a-code-generator)
  - [Why Domain Driven Design?](#why-domain-driven-design)
  - [Table of Contents](#table-of-contents)
  - [Generate complete Event Driven Microservices using DDD and API-First](#generate-complete-event-driven-microservices-using-ddd-and-api-first)
  - [What can we generate for you today?](#what-can-we-generate-for-you-today)
    - [Example: Generate a complete Backend Application from a JDL model](#example-generate-a-complete-backend-application-from-a-jdl-model)
    - [Describe your core business model using JDL:](#describe-your-core-business-model-using-jdl)
    - [Generate Backend Application](#generate-backend-application)
    - [Generate OpenAPI draft from JDL model](#generate-openapi-draft-from-jdl-model)
    - [Generate AsyncAPI definition from JDL model](#generate-asyncapi-definition-from-jdl-model)
    - [Configure ZenWave Maven Plugin for AsyncAPI generation](#configure-zenwave-maven-plugin-for-asyncapi-generation)
    - [Spring REST Controllers from OpenAPI](#spring-rest-controllers-from-openapi)
    - [Integration Test for your Controllers using Spring WebTestClient](#integration-test-for-your-controllers-using-spring-webtestclient)
    - [E2E and Contract Testing](#e2e-and-contract-testing)
      - [KarateDSL Ent-to-End tests for REST APIs (using sister project ZenWave KarateIDE)](#karatedsl-ent-to-end-tests-for-rest-apis-using-sister-project-zenwave-karateide)
      - [High Fidelity Stateful REST API Mocks (using sister project ZenWave ApiMock)](#high-fidelity-stateful-rest-api-mocks-using-sister-project-zenwave-apimock)
      - [AsyncAPI interfaces Mocks and Contract Tests (ToBeDefined)](#asyncapi-interfaces-mocks-and-contract-tests-tobedefined)
- [Refactoring a legacy monolith](#refactoring-a-legacy-monolith)
    - [Reverse engineering JDL from Java classes (JPA and MongoDB)](#reverse-engineering-jdl-from-java-classes-jpa-and-mongodb)
- [Adding functionality on top of an existent microservices architecture](#adding-functionality-on-top-of-an-existent-microservices-architecture)
    - [Reverse engineering JDL from Java classes (JPA and MongoDB)](#reverse-engineering-jdl-from-java-classes-jpa-and-mongodb-1)
    - [Reverse engineering JDL from OpenAPI definition schemas](#reverse-engineering-jdl-from-openapi-definition-schemas)
<!-- TOC -->

## Generate complete Event Driven Microservices using DDD and API-First

You can generate complete Event Driven Microservices using DDD and API-First

Follow instructions in [Getting Started](https://zenwave360.github.io/docs/getting-started/)

> 👉 Describe your Model → Generate Backend ⤳ Generate OpenAPI ⤳ Generate AsyncAPI → Generate API Implementations → Generate Tests and Contracts 👍

1. Start by describing your core domain model using JDL entities and relationships, annotations and comments.
2. Generate a complete Backend Application from your Domain Definition Model.
3. Generate a draft OpenAPI definition from the JDL model. Edit collaboratively this OpenAPI document and then generate some more functional code and tests from that definition.
4. Generate a draft AsyncAPI definition for consuming async request commands and publishing domain events. Now use zenwave maven plugin to generate strongly typed business interfaces implementing some Enterprise Integration Patterns like: transactional outbox, business dead letter queue...
5. Generate E2E, Integration tests and Consumer Contracts for the public APIs you just produced.


![ZenWave Features MindMap](docs/ZenWave-MindMap.svg)


## What can we generate for you today?

Whether you are:

- [Designing a system from scratch](#designing-a-system-from-scratch-with-ddd-and-api-first),
- [Refactoring a legacy monolith](#refactoring-a-legacy-monolith) or just
- [Adding functionality on top of an existent microservices architecture](#adding-functionality-on-top-of-an-existent-microservices-archytecture)

...ZenWave SDK can... **generate a lot of code for you!!**

![06-ServiceImplementation-Hexagonal](docs/06-ServiceImplementation-Hexagonal.excalidraw.svg)

- [x] Standard Plugins
  - [x] JDL Backend Application (flexible hexagonal architecture)
    - [x] Domain Entities,
    - [x] Inbound
      - [x] Service Ports, DTOs, Mappers
      - [x] Implementation for CRUD operations
      - [x] Acceptance Tests: SpringData InMemory Repositories
    - [x] Outbound: SpringData Repositories, ElasticSearch... (for REST or Async see other plugins)
    - [x] Adapters:
      - [x] Spring MVC
      - [ ] ~~Spring WebFlux~~
    - [x] Flavors
      - [x] MongoDB
        - [x] Imperative
        - [ ] ~~Reactive~~
      - [x] JPA
        - [x] Imperative
        - [ ] ~~Reactive~~
    - [x] Unit/Integration Testing
      - [x] Edge Integration Testing: partial spring-boot context for outbound adapters (with testcontainers)
      - [x] Sociable Vertical Testing: manual dependency setup with in memory infrastructure _test-doubles_
      - [x] Vertical Integration Testing: full spring-boot context for inbound adapters (with testcontainers)
  - [x] JDL OpenAPI Controllers
  - [x] OpenAPI to Spring WebTestClient
  - [x] AsyncAPI Spring Cloud Streams3
    - [x] Consumer and Producer. Imperative and Reactive.
      - [x] Business Exceptions Dead Letter Queues Routing
    - [x] Producer with Transactional Outbox pattern
      - [x] For MongoDB
      - [x] For JDBC
    - [x] Enterprise Envelop Pattern
    - [x] Automatically fill headers at runtime from payload paths, tracing-id supplier...
  - [x] JDL to Specs
    - [x] JDL to OpenAPI
    - [x] JDL to AsyncAPI
      - [x] AsyncAPI schemas
      - [x] AVRO schemas
  - [x] API Testing
    - [x] KarateDSL
      - [x] OpenAPI to Karate E2E Tests (please use [KarateIDE VSCode Extension](https://github.com/ZenWave360/karate-ide) instead)
      - [x] OpenAPI to Karate/ApiMock Stateful Mocks (please use [KarateIDE VSCode Extension](https://github.com/ZenWave360/karate-ide) and [ZenWave ApiMock](https://github.com/ZenWave360/zenwave-apimock) instead)
    - [x] OpenAPI to Spring WebTestClient
    - [x] OpenAPI to REST-assured
    - [ ] ~~OpenAPI to Pact (_postponed sine die_)~~
  - [x] Reverser Engineering
    - [x] OpenAPI 2 JDL
    - [x] Java 2 JDL
      - [x] Spring Data MongoDB annotations
      - [x] JPA annotations

### Generate a Complete Backend Implementation: Getting Started

Follow instructions in [Getting Started](https://zenwave360.github.io/docs/getting-started/)
