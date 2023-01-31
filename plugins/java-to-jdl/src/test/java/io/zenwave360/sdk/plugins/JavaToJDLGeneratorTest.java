package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Test;

public class JavaToJDLGeneratorTest {

    @Test
    public void test_jpa_to_jdl() throws Exception {
        String jdl = new JavaToJDLGenerator()
                .withPackageName("io.zenwave360.sdk.jpa2jdl")
                .withPersistenceType(JavaToJDLGenerator.PersistenceType.JPA)
                .generate();
        System.out.println(jdl);
    }

    @Test
    public void test_mongodb_to_jdl() throws Exception {
        String jdl = new JavaToJDLGenerator()
                .withPackageName("io.zenwave360.sdk.mongodb2jdl")
                .withPersistenceType(JavaToJDLGenerator.PersistenceType.MONGODB)
                .generate();
        System.out.println(jdl);
    }
}
