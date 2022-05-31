package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.__NoOperationTemplateEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SpringCloudStreams3GeneratorTemplateNamesTest {

    String targetProperty = "_api";
    SpringCloudStreams3Generator mockedAsyncapiGenerator = new SpringCloudStreams3Generator() {
        @Override
        public TemplateEngine getTemplateEngine() {
            return new __NoOperationTemplateEngine();
        }
    }.withSourceProperty(targetProperty);

    private Map<String, ?> loadAsyncapiModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, ?> model = new DefaultYamlParser().withSpecFile(file.getAbsolutePath()).withTargetProperty(targetProperty).parse();
        return new AsyncApiProcessor().withTargetProperty(targetProperty).process(model);
    }

    @Test
    public void test_output_template_names_for_command_producer() throws Exception {
        Map<String, ?> model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml");

        mockedAsyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.CLIENT;
        mockedAsyncapiGenerator.apiPackage = "io.example.api";
        mockedAsyncapiGenerator.modelPackage = "io.example.api.model";
        List<TemplateOutput> outputTemplates = mockedAsyncapiGenerator.generate(model);
        Assertions.assertEquals(3, outputTemplates.size());
        Assertions.assertEquals("io/example/api/Header.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/IDefaultServiceCommandsProducer.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/DefaultServiceCommandsProducer.java", outputTemplates.get(2).getTargetFile());
    }

    @Test
    public void test_output_template_names_for_command_consumer() throws Exception {
        Map<String, ?> model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml");

        mockedAsyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.PROVIDER;
        List<TemplateOutput> outputTemplates = mockedAsyncapiGenerator.generate(model);
        Assertions.assertEquals(4, outputTemplates.size());
        Assertions.assertEquals("io/example/api/IDoCreateProduct.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/DoCreateProduct.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/IDoCreateProduct2.java", outputTemplates.get(2).getTargetFile());
        Assertions.assertEquals("io/example/api/DoCreateProduct2.java", outputTemplates.get(3).getTargetFile());
    }

    @Test
    public void test_output_template_names_for_events_producer() throws Exception {
        Map<String, ?> model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml");

        mockedAsyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.PROVIDER;
        mockedAsyncapiGenerator.apiPackage = "io.example.api";
        mockedAsyncapiGenerator.modelPackage = "io.example.api.model";
        List<TemplateOutput> outputTemplates = mockedAsyncapiGenerator.generate(model);
        Assertions.assertEquals(3, outputTemplates.size());
        Assertions.assertEquals("io/example/api/Header.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/IDefaultServiceEventsProducer.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/DefaultServiceEventsProducer.java", outputTemplates.get(2).getTargetFile());
    }

    @Test
    public void test_output_template_names_for_events_consumer() throws Exception {
        Map<String, ?> model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml");

        mockedAsyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.CLIENT;
        List<TemplateOutput> outputTemplates = mockedAsyncapiGenerator.generate(model);
        Assertions.assertEquals(4, outputTemplates.size());
        Assertions.assertEquals("io/example/api/IOnProductCreated.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/OnProductCreated.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/IOnProductCreated2.java", outputTemplates.get(2).getTargetFile());
        Assertions.assertEquals("io/example/api/OnProductCreated2.java", outputTemplates.get(3).getTargetFile());
    }
}
