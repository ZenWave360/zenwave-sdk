# Java 2 AsyncAPI Reverse Engineering
> 👉 ZenWave360 Helps You Create Software Easy to Understand

If you are using Spring Modulith Events and want to generate an AsyncAPI definition from your event producer class, you can use this plugin.

Then you can use [AsyncAPI and Spring Cloud Stream 3](../asyncapi-spring-cloud-streams3/) to generate all boilerplate code for publishing events using Spring Cloud Stream, including support for a [built-in Transactional Outbox pattern](https://www.zenwave360.io/posts/TransactionalOutBoxWithAsyncAPIAndSpringModulith/).

Inspecting your Java classes requires access to your project classpath. Add this dependency to your pom.xml:

> **Note**: Starting with version 2.0.0, the Maven `groupId` has changed to `io.zenwave360`. The code remains fully compatible.

```xml
<dependency>
    <groupId>io.zenwave360.sdk.plugins</groupId>
    <artifactId>java-to-asyncapi</artifactId>
    <version>${zenwave.version}</version>
    <scope>test</scope>
</dependency>
```

And then just paste the following code snippets on any test class or main method:


```java
public class JavaEventsToAsyncAPI {

    public static void main(String[] args) throws IOException {
        String asyncapi = new JavaToAsyncAPIGenerator()
                .withEventProducerClass(EventPublisher.class) // <-- your event publisher class
                .withAsyncapiVersion(AsyncapiVersionType.v3)
                .withTargetFile("target/asyncapi.yml")
                .generate();
        System.out.println(asyncapi); // printing for debug purposes
    }

}
```

```java
public class JavaEventsToAsyncAPI {

    public static void main(String[] args) throws IOException {
        String asyncapi = new JavaToAsyncAPIGenerator()
                .withEventProducerClass(EventPublisher.class) // <-- your event publisher class
                .withTargetFile("target/asyncapi-avro.yml")
                .withSchemaFormat(JavaToAsyncAPIGenerator.SchemaFormat.avro)
                .generate();
        System.out.println(asyncapi); // printing for debug purposes
    }

}
```
