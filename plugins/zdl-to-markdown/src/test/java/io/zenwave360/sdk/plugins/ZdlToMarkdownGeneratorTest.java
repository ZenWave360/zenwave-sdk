package io.zenwave360.sdk.plugins;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.ZDLParser;

public class ZdlToMarkdownGeneratorTest {

    private static final String CUSTOMER_ADDRESS_GLOSSARY_SHA256 = "117D4728FF536E09B8A99054CA6B9EE4C2797767C1607B72C7C1BD0FA6D1F778";
    private static final String CUSTOMER_ADDRESS_PLANTUML_SHA256 = "7AFC1851D24E9BC3316DD1DE448725DF7E27F016D2FA3B7B0CACCEA65937E75A";
    private static final String CUSTOMER_ADDRESS_RELATIONAL_GLOSSARY_SHA256 = "BEF8EBC2CC70D2D839A59BC82D762C1E762C4081707855A9A349BB4B1A8C07D8";
    private static final String ORDERS_WITH_AGGREGATE_GLOSSARY_SHA256 = "2D8F81E4D5FC5E7C16FC31425A7C4A5ED144593BC758C9D035468843670FFC3E";
    private static final String ORDERS_WITH_AGGREGATE_PLANTUML_SHA256 = "E452CD8FE9B8D7C3A1DFD84237F1530F861404103F6C1A254937AC8CD3F1556E";
    private static final String CUSTOMER_ENTITY_UML_SHA256 = "D4139ABBE94E6F616DECDBC6C98EBC0228BD08E30091D18B731AC361A0A3F06D";
    private static final String CUSTOMER_ORDER_AGGREGATE_UML_SHA256 = "197282A0E7AECEB9BAB98E122798E21789F9D637585ED7EA35E7C2B42A816BA3";
    private static final String ORDER_FAULTS_GLOSSARY_SHA256 = "06B7AAB52486A52C9CF737F8D1ACFD497222D09D4AC73182007C20BCDC4CC9A6";

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Parser parser = new ZDLParser();


    @Test
    public void test_customer_address_zdl_to_glossary() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        assertGeneratedMarkdown("target/customer-address.md", markdown, CUSTOMER_ADDRESS_GLOSSARY_SHA256);
    }

    @Test
    public void test_customer_address_aggregates_zdl_to_plantuml() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-payment.zdl");
        String markdown = ZdlToMarkdownPlugin.generatePlantUML(content);
        assertGeneratedMarkdown("target/customer-address-plantuml.md", markdown, CUSTOMER_ADDRESS_PLANTUML_SHA256);
    }

    @Test
    public void test_customer_address_relational_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        assertGeneratedMarkdown("target/customer-address-relational.md", markdown, CUSTOMER_ADDRESS_RELATIONAL_GLOSSARY_SHA256);
    }

    @Test
    public void test_customer_address_relational_zdl_to_markdown_with_aggregates() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        assertGeneratedMarkdown("target/orders-with-aggregate.md", markdown, ORDERS_WITH_AGGREGATE_GLOSSARY_SHA256);
    }

    @Test
    public void test_orders_with_aggregate_zdl_to_plantuml() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String markdown = ZdlToMarkdownPlugin.generatePlantUML(content);
        assertGeneratedMarkdown("target/orders-with-aggregate-plantuml.md", markdown, ORDERS_WITH_AGGREGATE_PLANTUML_SHA256);
    }

    @Test
    public void test_customer_address_relational_zdl_to_entity() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateAggregateUML(content, "Customer");
        assertGeneratedMarkdown("target/customer-entity-uml.md", markdown, CUSTOMER_ENTITY_UML_SHA256);
    }

    @Test
    public void test_customer_address_relational_zdl_to_entity_with_aggregate() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String markdown = ZdlToMarkdownPlugin.generateAggregateUML(content, "CustomerOrderAggregate");
        assertGeneratedMarkdown("target/customer-order-aggregate-uml.md", markdown, CUSTOMER_ORDER_AGGREGATE_UML_SHA256);
    }


    @Test
    public void test_order_faults_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        assertGeneratedMarkdown("target/order-faults-attachments-model.md", markdown, ORDER_FAULTS_GLOSSARY_SHA256);
    }

    private void assertGeneratedMarkdown(String path, String markdown, String expectedSha256) throws Exception {
        System.out.println(markdown);
        FileUtils.write(new File(path), markdown, StandardCharsets.UTF_8);
        Assertions.assertEquals(expectedSha256, sha256(markdown), "Rendered markdown changed for " + path);
    }

    private String sha256(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
