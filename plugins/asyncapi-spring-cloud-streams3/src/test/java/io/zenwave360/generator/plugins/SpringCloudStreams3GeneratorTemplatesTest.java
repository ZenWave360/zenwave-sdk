package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SpringCloudStreams3GeneratorTemplatesTest {

    SpringCloudStreams3Generator asyncapiGenerator = new SpringCloudStreams3Generator();

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, Object> model = new DefaultYamlParser().parse(file);
        return new AsyncApiProcessor().process(model);
    }

    @Test
    public void test_output_template_names_for_command_producer() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-commands.yml");

        asyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.CLIENT;
        asyncapiGenerator.apiPackage = "io.example.api";
        asyncapiGenerator.modelPackage = "io.example.api.model";
        List<TemplateOutput> outputTemplates = asyncapiGenerator.generate(model);
        Assertions.assertEquals(3, outputTemplates.size());
        Assertions.assertEquals("io/example/api/Header.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/IDefaultServiceCommandsProducer.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/DefaultServiceCommandsProducer.java", outputTemplates.get(2).getTargetFile());
    }

    @Test
    public void test_output_template_names_for_command_consumer() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-commands.yml");

        asyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.PROVIDER;
        List<TemplateOutput> outputTemplates = asyncapiGenerator.generate(model);
        Assertions.assertEquals(4, outputTemplates.size());
        Assertions.assertEquals("io/example/api/IDoCreateProduct.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/DoCreateProduct.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/IDoCreateProduct2.java", outputTemplates.get(2).getTargetFile());
        Assertions.assertEquals("io/example/api/DoCreateProduct2.java", outputTemplates.get(3).getTargetFile());
    }

    @Test
    public void test_output_template_names_for_events_producer() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-events.yml");

        asyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.PROVIDER;
        asyncapiGenerator.apiPackage = "io.example.api";
        asyncapiGenerator.modelPackage = "io.example.api.model";
        List<TemplateOutput> outputTemplates = asyncapiGenerator.generate(model);
        Assertions.assertEquals(3, outputTemplates.size());
        Assertions.assertEquals("io/example/api/Header.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/IDefaultServiceEventsProducer.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/DefaultServiceEventsProducer.java", outputTemplates.get(2).getTargetFile());
        System.out.println(outputTemplates.get(1).getContent());
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("package io.example.api;"));
    }

    @Test
    public void test_output_template_names_for_events_consumer() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-events.yml");

        asyncapiGenerator.role = AbstractAsyncapiGenerator.RoleType.CLIENT;
        List<TemplateOutput> outputTemplates = asyncapiGenerator.generate(model);
        Assertions.assertEquals(4, outputTemplates.size());
        System.out.println(outputTemplates.get(0).getContent());
        System.out.println(outputTemplates.get(1).getContent());
        Assertions.assertEquals("io/example/api/IOnProductCreated.java", outputTemplates.get(0).getTargetFile());
        Assertions.assertEquals("io/example/api/OnProductCreated.java", outputTemplates.get(1).getTargetFile());
        Assertions.assertEquals("io/example/api/IOnProductCreated2.java", outputTemplates.get(2).getTargetFile());
        Assertions.assertEquals("io/example/api/OnProductCreated2.java", outputTemplates.get(3).getTargetFile());
    }
}
