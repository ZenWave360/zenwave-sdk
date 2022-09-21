package io.zenwave360.generator.plugins;

import static org.jsonschema2pojo.SourceType.JSONSCHEMA;
import static org.jsonschema2pojo.SourceType.YAMLSCHEMA;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.FileCodeWriterWithEncoding;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.codemodel.JCodeModel;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractAsyncapiGenerator;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.NamingUtils;
import io.zenwave360.jsonrefparser.$Ref;

public class AsyncApiJsonSchema2PojoGenerator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public String sourceProperty = "api";

    @DocumentedOption(description = "API Specification File")
    public String specFile;

    @DocumentedOption(description = "Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty")
    public List<String> messageNames = new ArrayList<>();

    @DocumentedOption(description = "JsonSchema2Pojo settings")
    public Map<String, String> jsonschema2pojo = new HashMap<>();

    @DocumentedOption(description = "Target folder to generate code to. If left empty, it will print to stdout.")
    public File targetFolder;

    public String originalRefProperty = "x--original-\\$ref";

    public AsyncApiJsonSchema2PojoGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);

        String operationIdsRegex = operationIds.isEmpty() ? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
        List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.channels[*][*][?(@.operationId" + operationIdsRegex + ")]");

        String messageNamesRegex = messageNames.isEmpty() ? "" : " =~ /(" + StringUtils.join(messageNames, "|") + ")/";
        List<Map<String, Object>> messages = JSONPath.get(operations, "$[*].x--messages[*][?(@.name" + operationIdsRegex + ")]", Collections.emptyList());
        List<Map<String, Object>> oneOfMessages = JSONPath.get(operations, "$[*].x--messages[*].oneOf[?(@.name" + operationIdsRegex + ")]", Collections.emptyList());

        List<Map<String, Object>> allMessages = new ArrayList<>();
        allMessages.addAll(messages);
        allMessages.addAll(oneOfMessages);

        try {
            targetFolder.mkdirs();
            generate(apiModel, messages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // for (final File file : this.getExtraFiles(this.settings, this.api.getFile().getParentFile())) {
        // config.setSource(Arrays.asList(file.toURI().toURL()).iterator());
        // if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
        // config.setSourceType(SourceType.YAMLSCHEMA);
        // } else {
        // config.setSourceType(SourceType.JSONSCHEMA);
        // }
        // Jsonschema2Pojo.generate(config, null);
        // }

        return Collections.emptyList();
    }

    public void generate(Model apiModel, List<Map<String, Object>> messages) throws IOException, URISyntaxException {
        File apiFile = new File(apiModel.getRefs().file.toURI());

        for (final Map<String, Object> message : messages) {
            final String name = (String) message.get("name");
            Map<String, Object> payload = (Map) message.get("payload");
            $Ref payloadRef = apiModel.getRefs().getOriginalRef(payload);
            String schemaFormat = (String) message.get("schemaFormat"); // TODO get also global schemaFormat
            AsyncApiProcessor.SchemaFormatType schemaFormatType = AsyncApiProcessor.SchemaFormatType.getFormat(schemaFormat);

            final JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(jsonschema2pojo);
            config.setTargetDirectory(targetFolder);
            config.setTargetPackage(modelPackage);

            String messageClassName = NamingUtils.asJavaTypeName(name); // TODO
            if (AsyncApiProcessor.SchemaFormatType.isNativeFormat(schemaFormatType)) {
                generateFromNativeFormat(config, payload, modelPackage, messageClassName);
            } else {
                generateFromJsonSchemaFile(config, payloadRef.getUrl(), modelPackage, messageClassName);
            }
        }
    }

    public void generateFromJsonSchemaFile(JsonSchema2PojoConfiguration config, URL url, String packageName, String className) throws IOException {
        config.setSource(List.of(url).iterator());
        config.setTargetPackage(packageName);
        if (config.getSourceType() == null) {
            SourceType sourceType = url.getFile().endsWith(".yml") || url.getFile().endsWith(".yaml") ? YAMLSCHEMA : JSONSCHEMA;
            config.setSourceType(sourceType);
        }
        Jsonschema2Pojo.generate(config, null);
    }

    public void generateFromNativeFormat(JsonSchema2PojoConfiguration config, Map<String, Object> payload, String packageName, String className) throws IOException {
        var json = this.convertToJson(payload, packageName);

        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
        var sourcesWriter = new FileCodeWriterWithEncoding(targetFolder, config.getOutputEncoding());
        var resourcesWriter = new FileCodeWriterWithEncoding(targetFolder, config.getOutputEncoding());
        var codeModel = new JCodeModel();
        mapper.generate(codeModel, className, packageName, json);
        codeModel.build(sourcesWriter, resourcesWriter);
    }

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    protected String convertToJson(final Map<String, Object> payload, final String packageName) throws JsonProcessingException {
        this.yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String yml = this.yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        yml = RegExUtils.replaceAll(yml, originalRefProperty + ": \".*#/components/schemas/", "javaType: \"" + packageName + ".");
        yml = RegExUtils.replaceAll(yml, "ref: \".*#/components/schemas/", "javaType: \"" + packageName + ".");
        Object jsonObject = this.yamlMapper.readTree(yml);
        return this.jsonMapper.writeValueAsString(jsonObject);
    }

}
