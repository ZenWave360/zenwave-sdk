package io.zenwave360.sdk.plugins;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.ZDLParser;

public class ZdlToMarkdownGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Parser parser = new ZDLParser();


    @Test
    public void test_customer_address_zdl_to_glossary() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_zdl_to_plantuml() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-payment.zdl");
        String markdown = ZdlToMarkdownPlugin.generatePlantUML(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-plantuml.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_relational_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-relational.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_relational_zdl_to_markdown_with_aggregates() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/orders-with-aggregate.md"), markdown, "UTF-8");
    }


    @Test
    public void test_customer_address_relational_zdl_to_task_list() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateTaskList(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-relational-task-list.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_relational_zdl_to_entity() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateAggregateUML(content, "Customer");
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-entity-uml.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_relational_zdl_to_entity_with_aggregate() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String markdown = ZdlToMarkdownPlugin.generateAggregateUML(content, "CustomerOrderAggregate");
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-order-aggregate-uml.md"), markdown, "UTF-8");
    }


    @Test
    public void test_order_faults_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        String markdown = ZdlToMarkdownPlugin.generateGlossary(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-relational.md"), markdown, "UTF-8");
    }

}
