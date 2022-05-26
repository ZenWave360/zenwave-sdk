package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AbstractJDLGeneratorTest {

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "jdl";
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        return new JDLParser().withSpecFile(file.getAbsolutePath()).withTargetProperty(targetProperty).parse();
    }

    private AbstractJDLGenerator newAbstractJDLGenerator() {
        return new AbstractJDLGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, ?> apiModel) {
                return null;
            }
        };
    }


    @Test
    public void test_todo() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/21-points.jh");
        AbstractJDLGenerator jdlGenerator = newAbstractJDLGenerator();
        List<TemplateOutput> generated = jdlGenerator.generate(model);
    }

}
