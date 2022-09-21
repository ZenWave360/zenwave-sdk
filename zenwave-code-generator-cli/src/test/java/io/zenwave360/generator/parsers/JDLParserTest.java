package io.zenwave360.generator.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.utils.JSONPath;

public class JDLParserTest {

    private File getClasspathResourceAsFile(String resource) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(resource).toURI());
    }

    @Test
    public void testParseJDL() throws URISyntaxException, IOException {
        String targetProperty = "_jdl";
        JDLParser parser = new JDLParser().withSpecFile("classpath:io/zenwave360/generator/resources/jdl/21-points.jh").withTargetProperty(targetProperty);
        long startTime = System.currentTimeMillis();
        Map<String, Object> model = (Map) parser.parse().get(targetProperty);
        System.out.println("JDLParser load time: " + (System.currentTimeMillis() - startTime));
        Assertions.assertNotNull(model);
        Assertions.assertEquals("Integer", JSONPath.get(model, "$.entities.Points.fields.exercise.type"));
    }
}
