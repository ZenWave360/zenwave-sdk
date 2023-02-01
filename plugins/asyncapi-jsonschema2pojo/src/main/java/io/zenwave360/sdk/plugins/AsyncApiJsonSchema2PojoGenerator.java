package io.zenwave360.sdk.plugins;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.jsonschema2pojo.SourceType.JSONSCHEMA;
import static org.jsonschema2pojo.SourceType.YAMLSCHEMA;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import io.zenwave360.sdk.utils.AsyncAPIUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.exception.ClassAlreadyExistsException;
import org.jsonschema2pojo.rules.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.codemodel.JCodeModel;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
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

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder;

    @DocumentedOption(description = "Source folder inside folder to generate code to.")
    public String sourceFolder = "src/main/java";

    private File targetSourceFolder;

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

        List<Map<String, Object>> allMessages = new ArrayList<>();
        if (AsyncAPIUtils.isV2(apiModel)) {
            String operationIdsRegex = operationIds.isEmpty() ? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
            List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.channels[*][*][?(@.operationId" + operationIdsRegex + ")]");

            List<Map<String, Object>> messages = JSONPath.get(operations, "$[*].x--messages[*][?(@.name" + operationIdsRegex + ")]", Collections.emptyList());
            List<Map<String, Object>> oneOfMessages = JSONPath.get(operations, "$[*].x--messages[*].oneOf[?(@.name" + operationIdsRegex + ")]", Collections.emptyList());
            allMessages.addAll(messages);
            allMessages.addAll(oneOfMessages);
        }
        if (AsyncAPIUtils.isV3(apiModel)) {
            if (!messageNames.isEmpty()) {
                String messageNamesRegex = messageNames.isEmpty() ? "" : " =~ /(" + StringUtils.join(messageNames, "|") + ")/";
                Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.components.messages[*][?(@.name" + messageNamesRegex + ")]"));
                allMessages.addAll(messages);
            } else {
                String operationIdsRegex = operationIds.isEmpty() ? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
                Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.operations." + operationIdsRegex + ".channel.messages[*]"));
                allMessages.addAll(messages);
            }
        }

        targetSourceFolder = new File(targetFolder, sourceFolder);

        try {
            targetSourceFolder.mkdirs();
            generate(apiModel, allMessages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }

    public void generate(Model apiModel, List<Map<String, Object>> messages) throws IOException, URISyntaxException {
        var asyncapiVersion = JSONPath.get(apiModel, "$.asyncapi");
        var defaultSchemaFormat = AsyncApiProcessor.SchemaFormatType.ASYNCAPI_YAML.getSchemaFormat((String) asyncapiVersion);
        for (final Map<String, Object> message : messages) {
            Map<String, Object> payload = JSONPath.getFirst(message, "$.payload.schema", "$.payload");
            String name = (String)  ObjectUtils.firstNonNull(payload.get("x--schema-name"), message.get("name"));
            $Ref payloadRef = apiModel.getRefs().getOriginalRef(payload);
            var schemaFormatPath = AsyncAPIUtils.isV3(apiModel) ? "$.payload.schemaFormat" : "$.schemaFormat";
            var schemaFormat = (String) firstNonNull(JSONPath.get(message, schemaFormatPath), defaultSchemaFormat);
            AsyncApiProcessor.SchemaFormatType schemaFormatType = AsyncApiProcessor.SchemaFormatType.getFormat(schemaFormat);

            final JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(jsonschema2pojo);
            config.setTargetDirectory(targetSourceFolder);
            config.setTargetPackage(modelPackage);

            String messageClassName = NamingUtils.asJavaTypeName(name); // TODO
            if (AsyncApiProcessor.SchemaFormatType.isNativeFormat(schemaFormatType)) {
                generateFromNativeFormat(config, payload, modelPackage, messageClassName);
            } else {
                generateFromJsonSchemaFile(config, resolveClasspathURI(payloadRef.getURI()), modelPackage, messageClassName);
            }
        }
    }

    private URL resolveClasspathURI(URI classpathURI) throws MalformedURLException {
        if("classpath".equals(classpathURI.getScheme())) {
            return getClass().getClassLoader().getResource(classpathURI.toString().replaceFirst("classpath:/", ""));
        }
        return classpathURI.toURL();
    }
    public void generateFromJsonSchemaFile(JsonSchema2PojoConfiguration config, URL url, String packageName, String className) throws IOException {
        config.setSource(List.of(url).iterator());
        config.setTargetPackage(packageName);
        if (config.getSourceType() == null) {
            SourceType sourceType = url.getFile().endsWith(".yml") || url.getFile().endsWith(".yaml") ? YAMLSCHEMA : JSONSCHEMA;
            config.setSourceType(sourceType);
        }
        Jsonschema2Pojo.generate(config, ruleLogger);
    }

    public void generateFromNativeFormat(JsonSchema2PojoConfiguration config, Map<String, Object> payload, String packageName, String className) throws IOException {
        var json = this.convertToJson(payload, packageName);

        var ruleFactory = new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore());
        ruleFactory.setLogger(ruleLogger);
        SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());
        var sourcesWriter = new FileCodeWriterWithEncoding(targetSourceFolder, config.getOutputEncoding());
        var resourcesWriter = new FileCodeWriterWithEncoding(targetSourceFolder, config.getOutputEncoding());
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

    private RuleLogger ruleLogger = new AbstractRuleLogger() {
        @Override
        protected void doDebug(String msg) {
            log.debug(msg);
        }

        @Override
        protected void doError(String msg, Throwable e) {
            if (e instanceof ClassAlreadyExistsException) {
                log.debug("Class already exists: {}", ((ClassAlreadyExistsException)e).getExistingClass());
            } else {
                log.debug(msg, e);
            }
        }

        @Override
        protected void doInfo(String msg) {
            log.debug(msg);
        }

        @Override
        protected void doTrace(String msg) {
            log.trace(msg);
        }

        @Override
        protected void doWarn(String msg, Throwable e) {

        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }
    };
}
