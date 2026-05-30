package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JCodeModel;
import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.$Refs;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.apache.commons.lang3.ObjectUtils;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.exception.ClassAlreadyExistsException;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.NameHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.jsonschema2pojo.SourceType.JSONSCHEMA;
import static org.jsonschema2pojo.SourceType.YAMLSCHEMA;

public class AsyncApiJsonSchema2PojoGenerator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public String sourceProperty = "api";

    @DocumentedOption(description = "Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty")
    public List<String> messageNames = new ArrayList<>();

    @DocumentedOption(description = "JsonSchema2Pojo settings for downstream library", docLink = "https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-jsonschema2pojo/src/main/java/io/zenwave360/sdk/plugins/JsonSchema2PojoConfiguration.java")
    public Map<String, String> jsonschema2pojo = new HashMap<>();

    @DocumentedOption(description = "Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports.")
    public String generatedAnnotationClass;

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder;

    @DocumentedOption(description = "Source folder inside folder to generate code to.")
    public String sourceFolder = "src/main/java";

    private File targetSourceFolder;

    public String originalRefProperty = "x--original-\\$ref";

    public Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(sourceProperty);
    }

    @Override
    protected Templates configureTemplates() {
        return null;
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);

        var jsonMessages = AsyncAPIUtils.extractMessages(apiModel, AsyncApiProcessor.SchemaFormatType::isSchemaFormat, operationIds, messageNames);

        targetSourceFolder = new File(targetFolder, sourceFolder);

        try {
            targetSourceFolder.mkdirs();
            generate(apiModel, jsonMessages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new GeneratedProjectFiles();
    }

    public void generate(Model apiModel, List<Map<String, Object>> messages) throws IOException, URISyntaxException {
        var asyncapiVersion = JSONPath.get(apiModel, "$.asyncapi");
        var defaultSchemaFormat = AsyncApiProcessor.SchemaFormatType.ASYNCAPI_YAML.getSchemaFormat((String) asyncapiVersion);
        for (final Map<String, Object> message : messages) {
            Map<String, Object> payload = JSONPath.getFirst(message, "$.payload.schema", "$.payload");
            String name = (String)  ObjectUtils.firstNonNull(payload.get("x--schema-name"), message.get("name"));

            var schemaFormatPath = AsyncAPIUtils.isV3(apiModel) ? "$.payload.schemaFormat" : "$.schemaFormat";
            var schemaFormat = (String) firstNonNull(JSONPath.get(message, schemaFormatPath), defaultSchemaFormat);
            AsyncApiProcessor.SchemaFormatType schemaFormatType = AsyncApiProcessor.SchemaFormatType.getFormat(schemaFormat);

            final JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(jsonschema2pojo);
            config.setTargetDirectory(targetSourceFolder);
            config.setTargetPackage(modelPackage);

            String messageClassName = NamingUtils.asJavaTypeName(name); // TODO
            if (AsyncApiProcessor.SchemaFormatType.isNativeFormat(schemaFormatType)) {
                generateFromNativeFormat(apiModel, config, payload, modelPackage, messageClassName);
            } else {
                var payloadRef = getOriginalRef(apiModel.getRefs(), payload);
                generateFromJsonSchemaFile(config, resolveClasspathURI(payloadRef.getURI()), modelPackage, messageClassName);
            }
        }
    }

    public $Ref getOriginalRef($Refs refs, Object obj) {
        var originalRef = refs.getOriginalRef(obj);
        if (originalRef != null) {
            return originalRef;
        }
        return getReplacedRef(refs, obj);
    }

    public $Ref getReplacedRef($Refs refs, Object obj) {
        Object originalAllOf = refs.getReplacedRefsList();
        return refs.getReplacedRefsList().stream()
                .filter(pair -> isOriginalRef(obj, pair.getValue(), originalAllOf))
                .map(pair -> pair.getKey())
                .findFirst().orElse(null);
    }

    protected boolean isOriginalRef(Object value, Object savedValue, Object originalAllOf) {
        return value == savedValue || (originalAllOf != null && savedValue instanceof Map && ((Map<?, ?>) savedValue).get("allOf") == originalAllOf);
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

    public void generateFromNativeFormat(Model apiModel, JsonSchema2PojoConfiguration config, Map<String, Object> payload, String packageName, String className) throws IOException {
        var json = this.convertToJson(apiModel, config, payload, packageName);

        List<Annotator> annotators = new ArrayList<>();
        annotators.add(new AnnotatorFactory(config).getAnnotator(config.getAnnotationStyle()));
        Class<? extends Annotator> customAnnotatorClass = config.getCustomAnnotator();
        annotators.add(instantiate(customAnnotatorClass, config));
        if(generatedAnnotationClass != null) {
            annotators.add(new CustomAnnotator(config, generatedAnnotationClass, Map.of()));
        }

        var ruleFactory = new RuleFactory(config, new CompositeAnnotator(annotators.toArray(Annotator[]::new)), new SchemaStore());
        ruleFactory.setLogger(ruleLogger);
        SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());
        var sourcesWriter = new FileCodeWriterWithEncoding(targetSourceFolder, config.getOutputEncoding());
        var resourcesWriter = new FileCodeWriterWithEncoding(targetSourceFolder, config.getOutputEncoding());
        var codeModel = new JCodeModel();
        mapper.generate(codeModel, className, packageName, json);
        codeModel.build(sourcesWriter, resourcesWriter);
    }

    private final ObjectMapper jsonMapper = new ObjectMapper();

    protected String convertToJson(final Model apiModel, final JsonSchema2PojoConfiguration config, final Map<String, Object> payload, final String packageName) throws JsonProcessingException {
        populateJavaTypeFromRefsRecursively(apiModel, config, payload, packageName);
        return this.jsonMapper.writeValueAsString(payload);
    }

    private void populateJavaTypeFromRefsRecursively(Model apiModel, JsonSchema2PojoConfiguration config, Object obj, String packageName) {
        var nameHelper = new NameHelper(config);
        populateJavaTypeFromRefsRecursively(apiModel, nameHelper, obj, packageName);
    }

    private void populateJavaTypeFromRefsRecursively(Model apiModel, NameHelper nameHelper, Object obj, String packageName) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;

            // Replace $ref and original ref with javaType (if not already present)
            if (!map.containsKey("javaType")) {
                String refValue = JSONPath.getFirst(map, "$['" + originalRefProperty + "']", "$['$ref']");

                if (refValue != null && refValue.contains("#/components/schemas/")) {
                    String className = getRefClassName(apiModel, nameHelper, refValue);
                    map.put("javaType", packageName + "." + className);
                }
            }
            map.remove("x--original-$ref");
            map.remove("$ref");


            // Recursively process all values in the map
            for (Object value : map.values()) {
                populateJavaTypeFromRefsRecursively(apiModel, nameHelper, value, packageName);
            }

        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                populateJavaTypeFromRefsRecursively(apiModel, nameHelper, item, packageName);
            }
        }
    }

    private String getRefClassName(Model apiModel, NameHelper nameHelper, String refValue) {
        String schemaName = refValue.substring(refValue.lastIndexOf("/") + 1);
        try {
            JsonNode schemaNode = getReferencedSchemaNode(apiModel, schemaName);
            return getNormalizedClassName(nameHelper, schemaName, schemaNode);
        } catch (Exception e) {
            log.debug("Falling back to legacy ref class naming for ref {}", refValue, e);
            return NamingUtils.asJavaTypeName(schemaName);
        }
    }

    private String getNormalizedClassName(NameHelper nameHelper, String schemaName, JsonNode schemaNode) {
        String className = nameHelper.getClassName(schemaName, schemaNode);
        className = nameHelper.replaceIllegalCharacters(className);
        return nameHelper.normalizeName(className);
    }

    private JsonNode getReferencedSchemaNode(Model apiModel, String schemaName) {
        Map<String, Object> schemas = JSONPath.get(apiModel, "$.components.schemas");
        if (schemas == null) {
            return null;
        }
        Object schema = schemas.get(schemaName);
        if (schema == null) {
            return null;
        }
        return jsonMapper.valueToTree(schema);
    }

    private Annotator instantiate(Class<? extends Annotator> annotatorClass, GenerationConfig config) {
        try {
            return annotatorClass.getDeclaredConstructor(GenerationConfig.class).newInstance(config);
        } catch (Exception e) {
            try {
                return annotatorClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
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
