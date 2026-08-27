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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jsonschema2pojo.SourceType.JSONSCHEMA;
import static org.jsonschema2pojo.SourceType.YAMLSCHEMA;

public class AsyncApiJsonSchema2PojoGenerator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public String sourceProperty = "api";

    @DocumentedOption(description = "Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty")
    public List<String> messageNames = new ArrayList<>();

    @DocumentedOption(description = "JsonSchema2Pojo settings for downstream library", docLink = "https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/asyncapi-jsonschema2pojo/src/main/java/io/zenwave360/sdk/plugins/JsonSchema2PojoConfiguration.java")
    public Map<String, String> jsonschema2pojo = new HashMap<>();

    @DocumentedOption(description = "Generate POJOs for message binding keys (e.g. Kafka `bindings.kafka.key`) declared in selected messages.")
    public boolean generateMessageKeys = false;

    @DocumentedOption(description = "Generate POJOs for application headers declared on selected messages or inherited from message traits.")
    public boolean generateMessageHeaders = false;

    @DocumentedOption(description = "Generate POJOs for protocol binding header schemas (HTTP/JMS/MQTT) declared in selected messages.")
    public boolean generateBindingHeaders = false;

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

        // Schema format is evaluated per message schema root. A JSON Schema key may,
        // for example, belong to a message whose payload is Avro.
        var jsonMessages = AsyncAPIUtils.extractMessages(apiModel, ignored -> true, operationIds, messageNames);

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
        Set<String> generatedSchemaRoots = new LinkedHashSet<>();
        Map<String, String> generatedClassFqns = new LinkedHashMap<>();

        // Generate in a stable order so that, when two schema roots resolve to the same class
        // name, the winner is deterministic instead of depending on message hash-set ordering.
        List<Map<String, Object>> orderedMessages = new ArrayList<>(messages);
        orderedMessages.sort(Comparator.comparing(message -> String.valueOf(message.get("name"))));

        for (final Map<String, Object> message : orderedMessages) {
            for (SchemaRoot schemaRoot : extractSchemaRoots(apiModel, message, defaultSchemaFormat)) {
                if (!schemaRoot.generateScalarSchema() && !isPojoSchema(schemaRoot.schema())) {
                    continue;
                }

                AsyncApiProcessor.SchemaFormatType schemaFormatType = AsyncApiProcessor.SchemaFormatType.getFormat(schemaRoot.schemaFormat());
                if (!AsyncApiProcessor.SchemaFormatType.isSchemaFormat(schemaFormatType)) {
                    continue;
                }

                String deduplicationKey = getDeduplicationKey(apiModel, schemaRoot);
                if (!generatedSchemaRoots.add(deduplicationKey)) {
                    continue;
                }

                String className = getClassName(schemaRoot);
                String classFqn = getClassFqn(schemaRoot, className);
                String previousSource = generatedClassFqns.putIfAbsent(classFqn, deduplicationKey);
                if (previousSource != null) {
                    log.warn("Skipping schema '{}': class {} was already generated from '{}'. "
                            + "Rename one of the schemas or set a distinct javaType to avoid the name clash.",
                            deduplicationKey, classFqn, previousSource);
                    continue;
                }

                generateSchemaRoot(apiModel, schemaRoot, schemaFormatType, className);
            }
        }
    }

    protected List<SchemaRoot> extractSchemaRoots(Model apiModel, Map<String, Object> message, String defaultSchemaFormat) {
        List<SchemaRoot> roots = new ArrayList<>();
        String messageName = (String) ObjectUtils.getIfNull(message.get("name"), "Message");

        String payloadSchemaFormat = AsyncAPIUtils.isV3(apiModel)
                ? JSONPath.get(message, "$.payload.schemaFormat", defaultSchemaFormat)
                : JSONPath.get(message, "$.schemaFormat", defaultSchemaFormat);
        addSchemaRoot(roots, message.get("payload"), payloadSchemaFormat, messageName, true);

        if (generateMessageHeaders) {
            addSchemaRoot(roots, message.get("headers"), defaultSchemaFormat, messageName + "Headers", true);
        }

        if (generateMessageKeys) {
            addBindingSchemaRoot(roots, message, "$.bindings.kafka.key", defaultSchemaFormat, messageName + "Key");
        }

        if (generateBindingHeaders) {
            addBindingSchemaRoot(roots, message, "$.bindings.http.headers", defaultSchemaFormat, messageName + "HttpHeaders");
            addBindingSchemaRoot(roots, message, "$.bindings.jms.headers", defaultSchemaFormat, messageName + "JmsHeaders");
            addBindingSchemaRoot(roots, message, "$.bindings.mqtt.correlationData", defaultSchemaFormat, messageName + "MqttCorrelationData");
            addBindingSchemaRoot(roots, message, "$.bindings.mqtt.responseTopic", defaultSchemaFormat, messageName + "MqttResponseTopic");
        }

        return roots;
    }

    private void addBindingSchemaRoot(List<SchemaRoot> roots, Map<String, Object> message, String path, String defaultSchemaFormat, String fallbackName) {
        addSchemaRoot(roots, JSONPath.get(message, path), defaultSchemaFormat, fallbackName, false);
    }

    private void addSchemaRoot(List<SchemaRoot> roots, Object schemaContainer, String defaultSchemaFormat, String fallbackName, boolean generateScalarSchema) {
        if (!(schemaContainer instanceof Map<?, ?>)) {
            return;
        }

        Map<String, Object> container = (Map<String, Object>) schemaContainer;
        Object nestedSchema = container.get("schema");
        Map<String, Object> schema = nestedSchema instanceof Map<?, ?>
                ? (Map<String, Object>) nestedSchema
                : container;
        String schemaFormat = (String) ObjectUtils.getIfNull(container.get("schemaFormat"), defaultSchemaFormat);
        roots.add(new SchemaRoot(schema, schemaFormat, fallbackName, generateScalarSchema));
    }

    private boolean isPojoSchema(Map<String, Object> schema) {
        return "object".equals(schema.get("type"))
                || schema.containsKey("properties")
                || schema.containsKey("javaType")
                || schema.containsKey("x--schema-name");
    }

    private String getDeduplicationKey(Model apiModel, SchemaRoot schemaRoot) {
        Object javaType = schemaRoot.schema().get("javaType");
        if (javaType != null) {
            return "javaType:" + javaType;
        }

        Object originalRef = schemaRoot.schema().get("x--original-$ref");
        if (originalRef != null) {
            return "ref:" + originalRef;
        }

        $Ref ref = getOriginalRef(apiModel.getRefs(), schemaRoot.schema());
        if (ref != null) {
            return "ref:" + ref.getURI();
        }
        return "inline:" + schemaRoot.fallbackName();
    }

    private String getClassName(SchemaRoot schemaRoot) {
        String schemaName = (String) ObjectUtils.firstNonNull(schemaRoot.schema().get("x--schema-name"), schemaRoot.fallbackName());
        return NamingUtils.asJavaTypeName(schemaName);
    }

    /**
     * Fully qualified name the generated class is expected to land at, used to detect name clashes.
     * A root-level {@code javaType} overrides both the model package and the derived class name.
     * Best-effort for external json-schema files, where jsonschema2pojo derives class names itself.
     */
    private String getClassFqn(SchemaRoot schemaRoot, String className) {
        Object javaType = schemaRoot.schema().get("javaType");
        return javaType != null ? javaType.toString() : modelPackage + "." + className;
    }

    private void generateSchemaRoot(Model apiModel, SchemaRoot schemaRoot, AsyncApiProcessor.SchemaFormatType schemaFormatType, String className) throws IOException {
        final JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(jsonschema2pojo);
        config.setTargetDirectory(targetSourceFolder);
        config.setTargetPackage(modelPackage);

        if (AsyncApiProcessor.SchemaFormatType.isNativeFormat(schemaFormatType)) {
            generateFromNativeFormat(apiModel, config, schemaRoot.schema(), modelPackage, className);
        } else {
            $Ref schemaRef = getOriginalRef(apiModel.getRefs(), schemaRoot.schema());
            if (schemaRef == null) {
                throw new IllegalArgumentException("External JSON Schema root has no original reference: " + className);
            }
            generateFromJsonSchemaFile(config, resolveClasspathURI(schemaRef.getURI()), modelPackage, className);
        }
    }

    protected record SchemaRoot(Map<String, Object> schema, String schemaFormat, String fallbackName, boolean generateScalarSchema) {
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
        Map<String, Object> payloadCopy = this.jsonMapper.convertValue(payload, Map.class);
        populateJavaTypeFromRefsRecursively(apiModel, config, payloadCopy, packageName);
        return this.jsonMapper.writeValueAsString(payloadCopy);
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

                if (refValue != null && refValue.startsWith("#/")) {
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
            JsonNode schemaNode = getReferencedSchemaNode(apiModel, refValue);
            return getNormalizedClassName(nameHelper, schemaName, schemaNode);
        } catch (Exception e) {
            log.debug("Falling back to legacy ref class naming for ref {}", refValue, e);
            return getNormalizedClassName(nameHelper, schemaName, null);
        }
    }

    private String getNormalizedClassName(NameHelper nameHelper, String schemaName, JsonNode schemaNode) {
        String className = nameHelper.getClassName(schemaName, schemaNode);
        className = nameHelper.replaceIllegalCharacters(className);
        return nameHelper.normalizeName(className);
    }

    private JsonNode getReferencedSchemaNode(Model apiModel, String refValue) {
        Object schema = null;
        if (refValue.startsWith("#/components/schemas/")) {
            String schemaName = refValue.substring(refValue.lastIndexOf("/") + 1);
            Map<String, Object> schemas = JSONPath.get(apiModel, "$.components.schemas");
            if (schemas != null) {
                schema = schemas.get(schemaName);
            }
        }
        if (schema == null) {
            schema = getResolvedRefValue(apiModel.getRefs(), refValue);
        }
        if (schema == null) {
            schema = getInternalRefValue(apiModel, refValue);
        }
        if (schema == null) {
            return null;
        }
        return jsonMapper.valueToTree(schema);
    }

    private Object getResolvedRefValue($Refs refs, String refValue) {
        Object original = refs.getOriginalRefsList().stream()
                .filter(pair -> refValue.equals(pair.getKey().getRef()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (original != null) {
            return original;
        }
        return refs.getReplacedRefsList().stream()
                .filter(pair -> refValue.equals(pair.getKey().getRef()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Object getInternalRefValue(Model apiModel, String refValue) {
        if (!refValue.startsWith("#/")) {
            return null;
        }
        Object current = apiModel.model();
        String[] tokens = refValue.substring(2).split("/");
        for (String token : tokens) {
            String key = token.replace("~1", "/").replace("~0", "~");
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else if (current instanceof List<?> list) {
                try {
                    current = list.get(Integer.parseInt(key));
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
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
