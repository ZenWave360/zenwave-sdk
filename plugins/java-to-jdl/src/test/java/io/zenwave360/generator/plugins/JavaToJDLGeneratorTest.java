package io.zenwave360.generator.plugins;

import org.junit.jupiter.api.Test;

public class JavaToJDLGeneratorTest {

    @Test
    public void test_jpa_to_jdl() throws Exception {
        String jdl = new JavaToJDLGenerator()
                .withPackageName("io.zenwave360.generator.jpa2jdl")
                .withPersistenceType(JavaToJDLGenerator.PersistenceType.JPA)
                .generate();
        System.out.println(jdl);
    }

    @Test
    public void test_mongodb_to_jdl() throws Exception {
        String jdl = new JavaToJDLGenerator()
                .withPackageName("io.zenwave360.generator.mongodb2jdl")
                .withPersistenceType(JavaToJDLGenerator.PersistenceType.MONGODB)
                .generate();
        System.out.println(jdl);
    }
}
