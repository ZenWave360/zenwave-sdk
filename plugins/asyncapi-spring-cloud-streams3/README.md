# AsyncAPI and Spring Cloud Stream 3
> 👉 ZenWave360 Helps You Create Software that's Easy to Understand

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zenwave360.zenwave-sdk/zenwave-sdk.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.github.zenwave360.zenwave-sdk/zenwave-sdk)
[![GitHub](https://img.shields.io/github/license/ZenWave360/zenwave-sdk)](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE)

![AsyncAPI and Spring Cloud Streams 3](../../docs/ZenWave360-AsyncAPI-SpringCloudStreams.excalidraw.svg)

<!-- TOC -->
* [AsyncAPI and Spring Cloud Stream 3](#asyncapi-and-spring-cloud-stream-3)
  * [Generating Consumer & Producer APIs](#generating-consumer--producer-apis)
    * [Getting Help](#getting-help)
    * [Options](#options)
  * [Producer Event-Captors for Tests (Mocks)](#producer-event-captors-for-tests--mocks-)
  * [Consumer Adapters API Tests](#consumer-adapters-api-tests)
    * [Options for Adapter Tests](#options-for-adapter-tests)
  * [Generating Consumer Adapters (Skeletons)](#generating-consumer-adapters--skeletons-)
    * [Options for Consumer Adapters](#options-for-consumer-adapters)
  * [Enterprise Integration Patterns](#enterprise-integration-patterns)
  * [Maven Plugin Plugin (API-First)](#maven-plugin-plugin--api-first-)
    * [Provider Imperative style without Transactional Outbox](#provider-imperative-style-without-transactional-outbox)
    * [Provider Imperative style with Mongodb Transactional Outbox](#provider-imperative-style-with-mongodb-transactional-outbox)
    * [Provider Imperative style with JDBC Transactional Outbox](#provider-imperative-style-with-jdbc-transactional-outbox)
    * [Model DTOs using `jsonschema2pojo` generator](#model-dtos-using-jsonschema2pojo-generator)
    * [Provider with AVRO schema payloads.](#provider-with-avro-schema-payloads)
    * [Client Imperative style.](#client-imperative-style)
    * [Client Reactive style.](#client-reactive-style)
<!-- TOC -->

## Generating Consumer & Producer APIs

With ZenWave's `spring-cloud-streams3` and `jsonschema2pojo` generator plugins you can generate:
- Strongly typed **business interfaces**
- **Payload DTOs** and 
- **Header objects** from AsyncAPI definitions.

It uses Spring Cloud Streams as default implementation so it can connect to many different brokers via provided binders.

And because everything is hidden behind interfaces we can encapsulate many Enterprise Integration Patterns:

- Transactional Outbox: with MongoDB ChangeStreams, Plain SQL and Debezium SQL flavors
- Business DeadLetter Queues: allowing you to route different business Exceptions to different DeadLetter queues for non-retrayable errors.
- Enterprise Envelope: when your organization uses a common Envelope for messages, you can still express your AsyncAPI definition in terms of your business payload.

Because APIs mediated by a broker are inherently **symmetrical** it's difficult to establish the roles of client/server: what represents a `publish` operation from one side will be a `subscribe` operation seen from the other side. Also, a given service can act as a publisher and subscriber on the same API.

For these reasons, to avoid defining the same API operations multiple times from each perspective, we propose to define de API only once from the perspective of the provider of the functionality, which may be a producer, a consumer or both. 

Some definitions:

- SERVICE: An independent piece of software, typically a microservice, that provides a set of capabilities to other services.
- PROVIDER: The service that implements the functionality of the API. It may be accepting asynchronous command request or publishing business domain events.
- CLIENT/s: The service/s that makes use of the functionality of the API. It may be requesting asynchronous commands or subscribing to business domain events.
- PRODUCER: A service that writes a given message.
- CONSUMER: A service that reads a given message.


Use the table to understand which section of AsyncAPI (publish or subscribe) to use for each topic, and which role (provider or client) to use on the plugin configuration.

|                              | Events                | Commands                |
|------------------------------|-----------------------|-------------------------|
| Provider                     | Produces (publish)    | Consumes (subscribe)    |
| Client                       | Consumes (subscribe)  | Produces (publish)      |
| OperationId Suggested Prefix | **on**&lt;Event Name> | **do**&lt;Command Name> |

### Getting Help

```shell
jbang zw -p io.zenwave360.sdkns.SpringCloudStreams3Plugin --help
```

### Options

| **Option**                      | **Description**                                                                                                                                                                         | **Type**                | **Default**          | **Values**           |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|----------------------|----------------------|
| `specFile`                      | API Specification File                                                                                                                                                                  | URI                     |                      |                      |
| `targetFolder`                  | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                              | File                    |                      |                      |
| `style`                         | Programming style                                                                                                                                                                       | ProgrammingStyle        | imperative           | imperative, reactive |
| `role`                          | Project role: provider/client                                                                                                                                                           | AsyncapiRoleType        | provider             | provider, client     |
| `exposeMessage`                 | Whether to expose underlying spring Message to consumers or not.                                                                                                                        | boolean                 | false                |                      |
| `apiPackage`                    | Java API package name for producerApiPackage and consumerApiPackage if not specified.                                                                                                   | String                  |                      |                      |
| `producerApiPackage`            | Java API package name for outbound (producer) services. It can override apiPackage for producers.                                                                                       | String                  | {{apiPackage}}       |                      |
| `consumerApiPackage`            | Java API package name for inbound (consumer) services. It can override apiPackage for consumer.                                                                                         | String                  | {{apiPackage}}       |                      |
| `modelPackage`                  | Java Models package name                                                                                                                                                                | String                  |                      |                      |
| `operationIds`                  | Operation ids to include in code generation. Generates code for ALL if left empty                                                                                                       | List                    | []                   |                      |
| `transactionalOutbox`           | Transactional outbox type for message producers.                                                                                                                                        | TransactionalOutboxType | none                 | none, mongodb, jdbc  |
| `useEnterpriseEnvelope`         | Include support for enterprise envelop wrapping/unwrapping.                                                                                                                             | boolean                 | false                |                      |
| `envelopeJavaTypeExtensionName` | AsyncAPI Message extension name for the envelop java type for wrapping/unwrapping.                                                                                                      | String                  | x-envelope-java-type |                      |
| `methodAndMessageSeparator`     | To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces. | String                  | $                    |                      |
| `consumerPrefix`                | SC Streams Binder class prefix                                                                                                                                                          | String                  |                      |                      |
| `consumerSuffix`                | SC Streams Binder class suffix                                                                                                                                                          | String                  | Consumer             |                      |
| `servicePrefix`                 | Business/Service interface prefix                                                                                                                                                       | String                  | I                    |                      |
| `serviceSuffix`                 | Business/Service interface suffix                                                                                                                                                       | String                  | ConsumerService      |                      |
| `bindingSuffix`                 | Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. <operation-id>-in-0                                                                             | String                  | -0                   |                      |
| `bindingTypes`                  | Binding names to include in code generation. Generates code for ALL bindings if left empty                                                                                              | List                    |                      |                      |
| `skipFormatting`                | Skip java sources output formatting                                                                                                                                                     | boolean                 | false                |                      |
| `haltOnFailFormatting`          | Halt on formatting errors                                                                                                                                                               | boolean                 | true                 |                      |

## Populating Headers at Runtime Automatically

ZenWave Code Generator provides `x-runtime-expression` for automatic header population at runtime. Values for this extension property are:

- `$message.payload#/<json pointer fragment>`: follows the same format as AsyncAPI [Correlation ID](https://www.asyncapi.com/docs/reference/specification/v2.5.0#correlationIdObject) object.
- `$tracingIdSupplier`: will use the tracing id `java.function.Supplier` configured in your Spring context.

```yaml
    CustomerEventMessage:
      name: CustomerEventMessage
      // [...] other properties omitted for brevity
      headers:
        type: object
        properties:
          kafka_messageKey:
            type: string
            description: This one will be populated automatically at runtime
            x-runtime-expression: $message.payload#/customer/id
          tracingId:
            type: string
            description: This one will be populated automatically at runtime
            x-runtime-expression: $tracingIdSupplier
```

```xml
<configOption>
    <tracingIdSupplierQualifier>myTracingIdSupplier</tracingIdSupplierQualifier><!-- default is "tracingIdSupplier" -->
    <runtimeHeadersProperty>x-custom-runtime-expression</runtimeHeadersProperty><!-- you can also override this extension property name -->
</configOption>
```

```java
    @Bean("myTracingIdSupplier")
    public Supplier tracingIdSupplier() {
        return () -> "test-tracing-id";
    }
```

## Producer Event-Captors for Tests (Mocks)

```java
// autogenerate in: target/generated-sources/zenwave/src/test/java/.../CustomerOrderEventsProducerCaptor.java
public class CustomerOrderEventsProducerCaptor implements ICustomerOrderEventsProducer {
    
    protected Map<String, List<Message>> capturedMessages = new HashMap<>();
    public Map<String, List<Message>> getCapturedMessages() {
        return capturedMessages;
    }
    // other details omitted for brevity
    
    /**
     * CustomerOrder Domain Events
     */
    public boolean onCustomerOrderEvent(CustomerOrderEventPayload payload, CustomerOrderEventPayloadHeaders headers) {
        log.debug("Capturing message to topic: {}", onCustomerOrderEventBindingName);
        Message message = MessageBuilder.createMessage(payload, new MessageHeaders(headers));
        return appendCapturedMessage(onCustomerOrderEventBindingName, message);
    }

}
```

```java
// autogenerated in: target/generated-sources/zenwave/src/test/java/.../ProducerInMemoryContext.java
public class ProducerInMemoryContext {

    public static final ProducerInMemoryContext INSTANCE = new ProducerInMemoryContext();


    private CustomerEventsProducerCaptor customerEventsProducerCaptor = new CustomerEventsProducerCaptor();

    public <T extends ICustomerEventsProducer> T customerEventsProducer() {
        return (T) customerEventsProducerCaptor;
    }
}
```

## Consumer Adapters API Tests


```shell
jbang zw -p io.zenwave360.sdkns.SpringCloudStreams3TestsPlugin \
    specFile=src/main/resources/model/asyncapi.yml \
    role=provider \
    style=imperative \
    basePackage=io.zenwave360.example \
    consumerApiPackage=io.zenwave360.example.adapters.events \
    modelPackage=io.zenwave360.example.core.domain.events \
    targetFolder=.
```

```java
// generated and editable in: src/test/java/.../adapters/events/DoCustomerRequestConsumerServiceIT.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.transaction.annotation.Transactional
public class DoCustomerRequestConsumerServiceIT extends BaseConsumerTest {

  @Autowired public IDoCustomerRequestConsumerService consumerService;

  /** Test for doCustomerRequest: */
  @Test
  public void doCustomerRequestTest() {
    CustomerRequestPayload payload = new CustomerRequestPayload();
    payload.setCustomerId(null);
    payload.setRequestType(null);
    payload.setCustomer(null);

    CustomerRequestPayloadHeaders headers = new CustomerRequestPayloadHeaders();

    // invoke the method under test
    consumerService.doCustomerRequest(payload, headers);
    // perform your assertions here
  }
}
```

### Options for Adapter Tests

| **Option**                     | **Description**                                                                                                                                                                         | **Type**         | **Default**                                              | **Values**           |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|----------------------------------------------------------|----------------------|
| `specFile`                     | API Specification File                                                                                                                                                                  | URI              |                                                          |                      |
| `targetFolder`                 | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                              | File             |                                                          |                      |
| `role`                         | Project role: provider/client                                                                                                                                                           | AsyncapiRoleType | provider                                                 | provider, client     |
| `style`                        | Programming style                                                                                                                                                                       | ProgrammingStyle | imperative                                               | imperative, reactive |
| `operationIds`                 | Operation ids to include in code generation. Generates code for ALL if left empty                                                                                                       | List             | []                                                       |                      |
| `apiPackage`                   | Java API package name for producerApiPackage and consumerApiPackage if not specified.                                                                                                   | String           |                                                          |                      |
| `testsPackage`                 | Package name for generated tests                                                                                                                                                        | String           | {{consumerApiPackage}}                                   |                      |
| `testSuffix`                   | Class name suffix for generated test classes                                                                                                                                            | String           | IT                                                       |                      |
| `transactional`                | Annotate tests as @Transactional                                                                                                                                                        | boolean          | true                                                     |                      |
| `transactionalAnnotationClass` | @Transactional annotation class name                                                                                                                                                    | String           | org.springframework.transaction.annotation.Transactional |                      |
| `exposeMessage`                | Whether to expose underlying spring Message to consumers or not.                                                                                                                        | boolean          | false                                                    |                      |
| `consumerApiPackage`           | Java API package name for inbound (consumer) services. It can override apiPackage for consumer.                                                                                         | String           | {{apiPackage}}                                           |                      |
| `modelPackage`                 | Java Models package name                                                                                                                                                                | String           |                                                          |                      |
| `methodAndMessageSeparator`    | To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces. | String           | $                                                        |                      |
| `consumerPrefix`               | SC Streams Binder class prefix                                                                                                                                                          | String           |                                                          |                      |
| `consumerSuffix`               | SC Streams Binder class suffix                                                                                                                                                          | String           | Consumer                                                 |                      |
| `servicePrefix`                | Business/Service interface prefix                                                                                                                                                       | String           | I                                                        |                      |
| `serviceSuffix`                | Business/Service interface suffix                                                                                                                                                       | String           | ConsumerService                                          |                      |
| `bindingTypes`                 | Binding names to include in code generation. Generates code for ALL bindings if left empty                                                                                              | List             |                                                          |                      |
| `skipFormatting`               | Skip java sources output formatting                                                                                                                                                     | boolean          | false                                                    |                      |
| `haltOnFailFormatting`         | Halt on formatting errors                                                                                                                                                               | boolean          | true                                                     |                      |


## Generating Consumer Adapters (Skeletons)


```shell
jbang zw -p io.zenwave360.sdkns.SpringCloudStreams3AdaptersPlugin \
    specFile=src/main/resources/model/asyncapi.yml \
    jdlFile=src/main/resources/model/orders-model.jdl \
    role=provider \
    style=imperative \
    basePackage=io.zenwave360.example \
    consumerApiPackage=io.zenwave360.example.adapters.events \
    modelPackage=io.zenwave360.example.core.domain.events \
    targetFolder=.
```

```java
@Component
public class DoCustomerRequestConsumerServiceAdapter implements IDoCustomerRequestConsumerService {

  private EventEntityMapper mapper;
  // TODO: private EntityUseCases service;

  @Autowired
  public void setEventEntityMapper(EventEntityMapper mapper) {
    this.mapper = mapper;
  }

  /** Customer Async Requests */
  public void doCustomerRequest(CustomerRequestPayload payload, CustomerRequestPayloadHeaders headers) {
    // TODO: service.doCustomerRequest(mapper.asEntity(payload));
  };
}
```

### Options for Consumer Adapters

| **Option**                           | **Description**                                                                                                                                                                         | **Type**         | **Default**                               | **Values**           |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|-------------------------------------------|----------------------|
| `specFile`                           | API Specification File                                                                                                                                                                  | URI              |                                           |                      |
| `targetFolder`                       | Target folder to generate code to. If left empty, it will print to stdout.                                                                                                              | File             |                                           |                      |
| `specFiles`                          | JDL files to parse                                                                                                                                                                      | String[]         | [null]                                    |                      |
| `apiId`                              | Unique identifier of each AsyncAPI that you consume as a client or provider. It will become the last package token for generated adapters                                               | String           | provider                                  |                      |
| `style`                              | Programming style                                                                                                                                                                       | ProgrammingStyle | imperative                                | imperative, reactive |
| `role`                               | Project role: provider/client                                                                                                                                                           | AsyncapiRoleType | provider                                  | provider, client     |
| `operationIds`                       | Operation ids to include in code generation. Generates code for ALL if left empty                                                                                                       | List             | []                                        |                      |
| `exposeMessage`                      | Whether to expose underlying spring Message to consumers or not.                                                                                                                        | boolean          | false                                     |                      |
| `methodAndMessageSeparator`          | To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces. | String           | $                                         |                      |
| `basePackage`                        | Applications base package                                                                                                                                                               | String           |                                           |                      |
| `adaptersPackage`                    | The package to generate Async Inbound Adapters in                                                                                                                                       | String           | {{basePackage}}.adapters.events.{{apiId}} |                      |
| `apiPackage`                         | Java API package name for producerApiPackage and consumerApiPackage if not specified.                                                                                                   | String           |                                           |                      |
| `producerApiPackage`                 | Java API package name for outbound (producer) services. It can override apiPackage for producers.                                                                                       | String           | {{apiPackage}}                            |                      |
| `consumerApiPackage`                 | Java API package name for inbound (consumer) services. It can override apiPackage for consumer.                                                                                         | String           | {{apiPackage}}                            |                      |
| `modelPackage`                       | Java Models package name                                                                                                                                                                | String           |                                           |                      |
| `inboundDtosPackage`                 | Package where your inbound dtos are                                                                                                                                                     | String           | {{basePackage}}.core.inbound.dtos         |                      |
| `servicesPackage`                    | Package where your domain services/usecases interfaces are                                                                                                                              | String           | {{basePackage}}.core.inbound              |                      |
| `inputDTOSuffix`                     | Suffix for CRUD operations DTOs (default: Input)                                                                                                                                        | String           | Input                                     |                      |
| `jdlBusinessEntityProperty`          | Extension property referencing original jdl entity in components schemas (default: x-business-entity)                                                                                   | String           | x-business-entity                         |                      |
| `jdlBusinessEntityPaginatedProperty` | Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)                                                     | String           | x-business-entity-paginated               |                      |
| `consumerPrefix`                     | SC Streams Binder class prefix                                                                                                                                                          | String           |                                           |                      |
| `consumerSuffix`                     | SC Streams Binder class suffix                                                                                                                                                          | String           | Consumer                                  |                      |
| `servicePrefix`                      | Business/Service interface prefix                                                                                                                                                       | String           | I                                         |                      |
| `serviceSuffix`                      | Business/Service interface suffix                                                                                                                                                       | String           | ConsumerService                           |                      |
| `bindingSuffix`                      | Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. <operation-id>-in-0                                                                             | String           | -0                                        |                      |
| `bindingTypes`                       | Binding names to include in code generation. Generates code for ALL bindings if left empty                                                                                              | List             |                                           |                      |
| `skipFormatting`                     | Skip java sources output formatting                                                                                                                                                     | boolean          | false                                     |                      |
| `haltOnFailFormatting`               | Halt on formatting errors                                                                                                                                                               | boolean          | true                                      |                      |


## Enterprise Integration Patterns

Because access to the underlying broker is encapsulated behind the generated interfaces, it's possible to implement many Enterprise Integration Patterns (EIP) on top of them.

- [Transactional Outbox: for mongodb and jdbc](/Event-Driven-Architectures/Enterprise-Integration-Patterns/Transactional-Outbox)
- [Business DeadLetter Queue](/Event-Driven-Architectures/Enterprise-Integration-Patterns/Business-Dead-Letter-Queue)
- [Enterprise Envelope](/Event-Driven-Architectures/Enterprise-Integration-Patterns/Enterprise-Envelop)
- [Async Request/Response](/Event-Driven-Architectures/Enterprise-Integration-Patterns/Async-Request-Response) (coming soon)

## Maven Plugin Plugin (API-First)

You can use ZenWave Maven Plugin to generate code as part of your build process:

- Adding this generator jar as dependency to zenwave maven plugin.
- Passing plugin specific plugin as &lt;configOptions>.

```xml
<plugin>
    <groupId>io.github.zenwave360.zenwave-sdk</groupId>
    <artifactId>zenwave-sdk-maven-plugin</artifactId>
    <version>${zenwave.version}</version>
    <plugin>
        <addCompileSourceRoot>true</addCompileSourceRoot><!-- default is true -->
        <addTestCompileSourceRoot>true</addTestCompileSourceRoot><!-- default is true -->
    </plugin>
    <executions>
        <!-- Add executions for each generation here: -->
        <execution>
            <id>generate-asyncapi-xxx</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <plugin>
                <generatorName>spring-cloud-streams3</generatorName>
                <inputSpec>classpath:model/asyncapi.yml</inputSpec>
                <configOptions>
                    <!-- ... -->
                </configOptions>
            </plugin>
        </execution>
    </executions>
    
    <!-- add any sdk plugin (custom or standard) as dependency here -->
    <dependencies>
        <dependency>
            <groupId>io.github.zenwave360.zenwave-sdk.plugins</groupId>
            <artifactId>asyncapi-spring-cloud-streams3</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.zenwave360.zenwave-sdk.plugins</groupId>
            <artifactId>asyncapi-jsonschema2pojo</artifactId>
            <version>${zenwave.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### Provider Imperative style without Transactional Outbox

```xml
<execution>
    <id>generate-asyncapi-producer</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>classpath:model/asyncapi.yml</inputSpec>
        <configOptions>
            <role>provider</role>
            <style>imperative</style>
            <apiPackage>io.zenwave360.example.core.events.outbound.outbox.none</apiPackage>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```

### Provider Imperative style with Mongodb Transactional Outbox

```xml
<execution>
    <id>generate-asyncapi-producer-outbox-mongodb</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>classpath:model/asyncapi.yml</inputSpec>
        <configOptions>
            <role>provider</role>
            <style>imperative</style>
            <transactionalOutbox>mongodb</transactionalOutbox>
            <apiPackage>io.zenwave360.example.core.events.outbound.outbox.mongodb</apiPackage>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```

### Provider Imperative style with JDBC Transactional Outbox

```xml
<execution>
    <id>generate-asyncapi-producer-outbox-jdbc</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>classpath:model/asyncapi.yml</inputSpec>
        <configOptions>
            <role>provider</role>
            <style>imperative</style>
            <transactionalOutbox>jdbc</transactionalOutbox>
            <apiPackage>io.zenwave360.example.core.events.outbound.outbox.jdbc</apiPackage>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```

### Model DTOs using `jsonschema2pojo` generator

```xml
<execution>
    <id>generate-asyncapi-producer-dtos</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>jsonschema2pojo</generatorName>
        <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
        <configOptions>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```

### Provider with AVRO schema payloads.

```xml
<execution>
    <id>generate-asyncapi-producer-avro</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi-avro.yml</inputSpec>
        <configOptions>
            <role>provider</role>
            <style>imperative</style>
            <apiPackage>io.zenwave360.example.core.events.outbound.avro</apiPackage>
        </configOptions>
    </plugin>
</execution>

```

### Client Imperative style.

```xml
<execution>
    <id>generate-asyncapi-client-imperative</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
        <configOptions>
            <role>client</role>
            <style>imperative</style>
            <apiPackage>io.zenwave360.example.core.events.inbound.imperative</apiPackage>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```

### Client Reactive style.

```xml
<execution>
    <id>generate-asyncapi-client-reactive</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <plugin>
        <generatorName>spring-cloud-streams3</generatorName>
        <inputSpec>${pom.basedir}/src/main/resources/model/asyncapi.yml</inputSpec>
        <configOptions>
            <role>client</role>
            <style>reactive</style>
            <apiPackage>io.zenwave360.example.core.events.inbound.reactive</apiPackage>
            <modelPackage>io.zenwave360.example.core.events.model</modelPackage>
        </configOptions>
    </plugin>
</execution>
```
