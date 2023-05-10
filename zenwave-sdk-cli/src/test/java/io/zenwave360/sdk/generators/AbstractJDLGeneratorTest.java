package io.zenwave360.sdk.generators;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.templating.TemplateOutput;

public class AbstractJDLGeneratorTest {

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "jdl";
        return new JDLParser().withSpecFile(resource).withTargetProperty(targetProperty).parse();
    }

    private AbstractJDLGenerator newAbstractJDLGenerator() {
        return new AbstractJDLGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, Object> apiModel) {
                return null;
            }
        };
    }

    @Test
    public void test_todo() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/21-points.jh");
        AbstractJDLGenerator jdlGenerator = newAbstractJDLGenerator();
        List<TemplateOutput> generated = jdlGenerator.generate(model);
    }

}