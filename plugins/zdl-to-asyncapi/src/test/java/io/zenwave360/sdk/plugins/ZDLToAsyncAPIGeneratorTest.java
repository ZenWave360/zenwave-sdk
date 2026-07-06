package io.zenwave360.sdk.plugins;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public class ZDLToAsyncAPIGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    public void test_order_asyncapi_root_elements_places_servers_after_info() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("asyncapi", "3.1.0");
        document.put("info", Map.of("title", "Catalog Inventory"));
        document.put("channels", Map.of());
        document.put("components", Map.of());
        document.put("servers", Map.of("develop", Map.of("host", "localhost:9092")));

        Map<String, Object> ordered = AsyncAPIOverlayProcessor.orderAsyncAPIRootElements(document);

        Assertions.assertEquals(
            List.of("asyncapi", "info", "servers", "channels", "components"),
            new ArrayList<>(ordered.keySet())
        );
    }

    @Test
    public void test_zdl_to_asyncapi_v2() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.asyncapiVersion = AsyncapiVersionType.v2;
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }


    @Test
    public void test_zdl_to_asyncapi_v3() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }

    @Test
    public void test_zdl_to_asyncapi_relational_v3() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }

    @Test
    public void test_merge_customer_address_zdl_to_asyncapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        var generatedProjectFiles = generator.generate(model);
        AsyncAPIOverlayProcessor processor = new AsyncAPIOverlayProcessor();
        processor.asyncapiMergeFile = "classpath:/io/zenwave360/sdk/resources/asyncapi/asyncapi-merger.yml";
        processor.asyncapiOverlayFiles = List.of("classpath:/io/zenwave360/sdk/resources/asyncapi/asyncapi-overlay.yml");
        processor.process(generatedProjectFiles);

        List<TemplateOutput> outputTemplates = generatedProjectFiles.getAllTemplateOutputs();
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_remote_overlay_is_loaded_with_authentication() throws Exception {
        String overlay = """
                overlay: 1.1.0
                info:
                  title: Remote overlay
                  version: 1.0.0
                actions:
                  - target: $.info.title
                    update: Remote Overlay Applied
                """;
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/asyncapi-overlay.yml", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = overlay.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
            generatedProjectFiles.singleFiles.add(new TemplateOutput(
                    "asyncapi.yml",
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Base API
                      version: 1.0.0
                    channels: {}
                    """,
                    "YAML"));

            ZDLToAsyncAPIPlugin configuration = new ZDLToAsyncAPIPlugin();
            configuration.withOption("asyncapiOverlayFiles",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/asyncapi-overlay.yml");
            configuration.withAuthentication(List.of(new AuthenticationValue(
                    "Authorization",
                    "Bearer test-token",
                    AuthenticationValue.AuthenticationType.HEADER,
                    ignored -> true)));

            AsyncAPIOverlayProcessor processor = new AsyncAPIOverlayProcessor();
            MainGenerator.applyConfiguration(3, processor, configuration);

            processor.process(generatedProjectFiles);

            Map<String, Object> result = mapper.readValue(
                    generatedProjectFiles.singleFiles.get(0).getContent(), Map.class);
            Assertions.assertEquals("Remote Overlay Applied", JSONPath.get(result, "$.info.title"));
            Assertions.assertEquals("Bearer test-token", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_pass_through_additional_properties_are_forwarded_to_templates() throws Exception {
        // options that don't map to any declared field of the generator (as would be set inline on the CLI);
        // dotted keys build an object value for the x-server-id extension
        ZDLToAsyncAPIPlugin config = new ZDLToAsyncAPIPlugin();
        config.withOption("id", "urn:com.arcadiaeditions:orders:checkout:asyncapi");
        config.withOption("x-server-id.host", "localhost");
        config.withOption("x-server-id.port", "9092");

        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        MainGenerator.applyConfiguration(0, generator, config);

        // captured as pass-through additional properties (no field on the generator)
        Assertions.assertEquals("urn:com.arcadiaeditions:orders:checkout:asyncapi", generator.additionalProperties.get("id"));

        // flattened into the template model, including the object-valued x- extension
        Map<String, Object> model = generator.asConfigurationMap();
        Assertions.assertEquals("urn:com.arcadiaeditions:orders:checkout:asyncapi", model.get("id"));
        Map<String, Object> xServerId = (Map<String, Object>) model.get("x-server-id");
        Assertions.assertEquals("localhost", xServerId.get("host"));
        Assertions.assertEquals("9092", xServerId.get("port"));
    }

    @Test
    public void test_merge_customer_address_zdl_to_asyncapi_avro() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";
        generator.targetFile = "target/out/customer-address.avro.yml";
        generator.schemaFormat = ZDLToAsyncAPIGenerator.SchemaFormat.avro;

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        Assertions.assertEquals(11, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }
}
