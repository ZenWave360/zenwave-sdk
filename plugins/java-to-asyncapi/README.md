# Java 2 AsyncAPI Reverse Engineering
> 👉 ZenWave360 Helps You Create Software Easy to Understand

If you are using Spring Modulith Events and want to generate an AsyncAPI definition from your event producer class, you can use this plugin.

Then you can use [AsyncAPI and Spring Cloud Stream 3](../asyncapi-spring-cloud-streams3/) to generate all boilerplate code for publishing events using Spring Cloud Stream, including support for a [built-in Transactional Outbox pattern](https://www.zenwave360.io/posts/TransactionalOutBoxWithAsyncAPIAndSpringModulith/).

Inspecting your Java classes requires access to your project classpath. Add this dependency to your pom.xml:

```xml
<dependency>
    <groupId>io.github.zenwave360.zenwave-sdk.plugins</groupId>
    <artifactId>java-to-asyncapi</artifactId>
    <version>${zenwave.version}</version>
</dependency>
```

And then just paste the following code snippets on any test class or main method:


```java
String asyncapi = new JavaToAsyncAPIGenerator()
        .withEventProducerClass(EventProducer.class) // <-- your event producer class
        .withAsyncapiVersion(AsyncapiVersionType.v3)
        .withTargetFile("target/out/asyncapi.yml")
        .generate();
System.out.println(asyncapi); // printing only for debug purposes
```

```java
String asyncapi = new JavaToAsyncAPIGenerator()
        .withEventProducerClass(EventProducer.class) // <-- your event producer class
        .withTargetFile("target/out/asyncapi-avro.yml")
        .withSchemaFormat(JavaToAsyncAPIGenerator.SchemaFormat.avro)
        .generate();
System.out.println(asyncapi); // printing only for debug purposes
```
