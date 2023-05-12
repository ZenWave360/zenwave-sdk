package io.zenwave360.sdk.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.utils.JSONPath;

public class ZDLParserTest {

    private File getClasspathResourceAsFile(String resource) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(resource).toURI());
    }

    @Test
    public void testParseZDL() throws URISyntaxException, IOException {
        String targetProperty = "model";
        ZDLParser parser = new ZDLParser().withSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl").withTargetProperty(targetProperty);
        long startTime = System.currentTimeMillis();
        Map<String, Object> model = (Map) parser.parse().get(targetProperty);
        System.out.println("ZDLParser load time: " + (System.currentTimeMillis() - startTime));
        Assertions.assertNotNull(model);
        Assertions.assertEquals("String", JSONPath.get(model, "$.entities.Customer.fields.username.type"));
        Assertions.assertEquals("Customer", JSONPath.get(model, "$.services.CustomerService.methods.createCustomer.parameter"));
    }

    @Test
    public void testParseJDL() throws URISyntaxException, IOException {
        String targetProperty = "model";
        ZDLParser parser = new ZDLParser().withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/21-points.jh").withTargetProperty(targetProperty);
        long startTime = System.currentTimeMillis();
        Map<String, Object> model = (Map) parser.parse().get(targetProperty);
        System.out.println("ZDLParser load time: " + (System.currentTimeMillis() - startTime));
        Assertions.assertNotNull(model);
        Assertions.assertEquals("Integer", JSONPath.get(model, "$.entities.Points.fields.exercise.type"));
    }
}
