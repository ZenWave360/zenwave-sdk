package io.zenwave360.generator.plugins;

import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JDLEntitiesGeneratorTest {

    private Map<String, ?> loadJDLModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, ?> model = new JDLParser().withSpecFile(file.getAbsolutePath()).parse();
        return new JDLProcessor().process(model);
    }

    @Test
    public void test_entities() throws Exception {
        Map<String, ?> model = loadJDLModelFromResource("io/zenwave360/generator/resources/jdl/orders-model.jdl");
        JDLEntitiesGenerator generator = new JDLEntitiesGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);

        outputTemplates = new JavaFormatter().format(outputTemplates);

        for (TemplateOutput outputTemplate : outputTemplates) {
            System.out.println(" ----------- " + outputTemplate.getTargetFile());
            System.out.println(outputTemplate.getContent());
        }
    }

}
