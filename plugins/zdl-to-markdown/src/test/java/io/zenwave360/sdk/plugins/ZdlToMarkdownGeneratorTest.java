package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;

public class ZdlToMarkdownGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Parser parser = new ZDLParser();


    @Test
    public void test_customer_address_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        String markdown = ZdlToMarkdownPlugin.generateMarkdown(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address.md"), markdown, "UTF-8");
    }

    @Test
    public void test_customer_address_relational_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        String markdown = ZdlToMarkdownPlugin.generateMarkdown(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-relational.md"), markdown, "UTF-8");
    }

    @Test
    public void test_order_faults_zdl_to_markdown() throws Exception {
        String content = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        String markdown = ZdlToMarkdownPlugin.generateMarkdown(content);
        System.out.println(markdown);
        FileUtils.write(new File("target/customer-address-relational.md"), markdown, "UTF-8");
    }

}
