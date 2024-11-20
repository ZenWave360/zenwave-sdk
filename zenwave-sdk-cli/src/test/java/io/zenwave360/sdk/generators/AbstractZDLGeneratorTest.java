package io.zenwave360.sdk.generators;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.templating.TemplateOutput;

public class AbstractZDLGeneratorTest {

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "zdl";
        return new ZDLParser().withZdlFile(resource).withTargetProperty(targetProperty).parse();
    }

    private AbstractZDLGenerator newAbstractZDLGenerator() {
        return new AbstractZDLGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, Object> apiModel) {
                return null;
            }
        };
    }

    @Test
    public void test_todo() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        AbstractZDLGenerator generator = newAbstractZDLGenerator();
        List<TemplateOutput> generated = generator.generate(model);
    }

}
