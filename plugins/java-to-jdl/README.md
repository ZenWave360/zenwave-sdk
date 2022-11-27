> :point_right: ZenWave360 Helps You Create Software Easy to Understand

# Java 2 JDL Reverse Engineering

If starting with legacy project, you can reverse engineer JDL from Java entity classes. JPA and MongoDB are supported.

Inspecting your java classes requires access to your project classpath. Add this dependency to your pom.xml:

```xml
<dependency>
    <groupId>io.github.zenwave360.zenwave-code-generator.plugins</groupId>
    <artifactId>java-to-jdl</artifactId>
    <version>${zenwave.version}</version>
</dependency>
```

And then just paste the following code snippets on any test class or main method:


```java
String jdl = new JavaToJDLGenerator()
    .withPackageName("io.zenwave360.generator.jpa2jdl")
    .withPersistenceType(JavaToJDLGenerator.PersistenceType.JPA)
    .generate();
System.out.println(jdl);
```

```java
String jdl = new JavaToJDLGenerator()
    .withPackageName("io.zenwave360.generator.mongodb2jdl")
    .withPersistenceType(JavaToJDLGenerator.PersistenceType.MONGODB)
    .generate();
System.out.println(jdl);
```
