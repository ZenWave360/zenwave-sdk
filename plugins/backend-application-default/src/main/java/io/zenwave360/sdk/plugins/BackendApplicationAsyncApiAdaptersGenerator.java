package io.zenwave360.sdk.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

/**
 * Generates backend-owned implementations of consumer interfaces generated separately by
 * {@code asyncapi-generator}. It reads each referenced AsyncAPI model, but deliberately never
 * invokes AsyncAPI code generation: Maven must configure that plugin as a separate execution.
 */
public class BackendApplicationAsyncApiAdaptersGenerator extends AbstractAsyncapiGenerator {

    public enum MappingClassification {
        GENERATED,
        CUSTOM_REQUIRED
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ProjectLayout layout;

    public ProjectTemplates templates;

    @DocumentedOption(description = "Applications base package")
    public String basePackage;

    @DocumentedOption(description = "Whether to generate implementations for methods annotated with @asyncapi")
    public boolean implementEventListeners = false;

    @DocumentedOption(description = "Programming style. Only imperative listener contracts are currently implemented.")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Consumer interface prefix; must match asyncapi-generator.")
    public String consumerServicePrefix = "I";

    @DocumentedOption(description = "Consumer interface suffix; must match asyncapi-generator.")
    public String consumerServiceSuffix = "ConsumerService";

    @DocumentedOption(description = "Model class prefix; must match asyncapi-generator.")
    public String modelNamePrefix = "";

    @DocumentedOption(description = "Model class suffix; must match asyncapi-generator.")
    public String modelNameSuffix = "";

    private Model adapterApiModel;
    private String currentApiName;
    private String currentConsumerApiPackage;
    private String currentModelPackage;
    private int asyncApiCount;

    {
        templates = new BackendApplicationProjectTemplates();
        // Defaults mirror the established multi-API Maven convention. Explicit backend options or
        // per-api config can point at any packages used by asyncapi-generator.
        consumerApiPackage = "{{basePackage}}.client.{{apiId}}.events.consumer";
        modelPackage = "{{basePackage}}.client.{{apiId}}.events.dtos";
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        GeneratedProjectFiles files = new GeneratedProjectFiles();
        if (!implementEventListeners || style != ProgrammingStyle.imperative) {
            return files;
        }

        Map<String, Object> zdl = (Map<String, Object>) contextModel.get("zdl");
        Map<String, Map<String, Object>> apis = JSONPath.get(zdl, "$.apis", Map.of());
        List<Map<String, Object>> declaredAsyncApis = apis.values().stream()
                .filter(api -> Objects.equals("asyncapi", api.get("type")))
                .toList();
        asyncApiCount = declaredAsyncApis.size();
        List<Map<String, Object>> asyncApis = declaredAsyncApis.stream()
                .filter(api -> ZDLParser.getReferencedApiModel(api) != null)
                .toList();

        for (Map<String, Object> api : asyncApis) {
            files.addAll(generateApi(contextModel, api));
        }
        return files;
    }

    private GeneratedProjectFiles generateApi(Map<String, Object> contextModel, Map<String, Object> api) {
        currentApiName = (String) api.get("name");
        adapterApiModel = ZDLParser.getReferencedApiModel(api);
        configureReferencedApi(api);
        Map<String, Object> apiModel = apiTemplateModel(api);

        AsyncApiProcessor processor = new AsyncApiProcessor();
        processor.targetProperty = "api";
        processor.modelNamePrefix = modelNamePrefix;
        processor.modelNameSuffix = modelNameSuffix;
        processor.process(new HashMap<>(Map.of("api", adapterApiModel)));

        List<Map<String, Object>> consumerOperations = new ArrayList<>();
        consumerOperations.addAll(filterConsumerOperations(
                getSubscribeOperationsGroupedByTag(adapterApiModel), AsyncapiOperationType.subscribe));
        consumerOperations.addAll(filterConsumerOperations(
                getPublishOperationsGroupedByTag(adapterApiModel), AsyncapiOperationType.publish));

        List<Map<String, Object>> plannedOperations = consumerOperations.stream()
                .map(operation -> planOperation(contextModel, operation))
                .filter(Objects::nonNull)
                .toList();
        List<Map<String, Object>> generatedMappings = generatedMappings(plannedOperations);

        GeneratedProjectFiles files = new GeneratedProjectFiles();
        if (!generatedMappings.isEmpty()) {
            for (TemplateInput template : templates.asyncApiAdapterByApiTemplates) {
                files.singleFiles.add(generateTemplate(contextModel, template, apiModel,
                        Map.of("generatedMappings", generatedMappings)));
            }
        }
        for (Map<String, Object> channel : operationsByChannel(plannedOperations).values()) {
            for (TemplateInput template : templates.asyncApiAdapterByChannelTemplates) {
                files.singleFiles.add(generateTemplate(contextModel, template, apiModel, channel));
            }
        }
        return files;
    }

    private void configureReferencedApi(Map<String, Object> api) {
        role = AsyncapiRoleType.valueOf(Objects.toString(api.get("role"), AsyncapiRoleType.provider.name()));
    }

    private List<Map<String, Object>> filterConsumerOperations(
            Map<String, List<Map<String, Object>>> operationsByTags, AsyncapiOperationType operationType) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (operationsByTags == null) {
            return result;
        }
        OperationRoleType roleType = OperationRoleType.valueOf(role, operationType);
        if (roleType != null && !roleType.isProducer()) {
            operationsByTags.values().forEach(result::addAll);
        }
        return result;
    }

    /**
     * Emits a mapper only for one object message matched to exactly one local bean input. All
     * other shapes remain developer-owned compiling hooks.
     */
    private Map<String, Object> planOperation(Map<String, Object> contextModel, Map<String, Object> operation) {
        Map<String, Object> plannedOperation = new LinkedHashMap<>(operation);
        List<Map<String, Object>> messages = distinctMessages(operation);
        plannedOperation.put("adapterMessages", messages);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("classification", MappingClassification.CUSTOM_REQUIRED.name());
        plan.put("generated", false);

        Map<String, Object> zdl = (Map<String, Object>) contextModel.get("zdl");
        List<Map<String, Object>> matchingMethods = findServiceMethods(operation, zdl);
        if (matchingMethods.isEmpty()) {
            return null; // This referenced API operation is not implemented by this backend module.
        }
        if (matchingMethods.size() != 1) {
            return customPlan(plannedOperation, messages, plan,
                    "more than one ZDL service method matches the AsyncAPI operation");
        }

        Map<String, Object> serviceMethod = matchingMethods.get(0);
        addServiceDependency(plan, serviceMethod);
        if (messages.size() != 1) {
            return customPlan(plannedOperation, messages, plan,
                    "the operation does not have exactly one consumable message");
        }

        String parameterType = (String) serviceMethod.get("parameter");
        boolean hasOneBeanParameter = serviceMethod.get("paramId") == null
                && StringUtils.isNotBlank(parameterType)
                && JSONPath.get(serviceMethod, "$.options.paginated") == null
                && !JSONPath.get(serviceMethod, "$.parameterIsArray", false);
        if (!hasOneBeanParameter) {
            return customPlan(plannedOperation, messages, plan,
                    "the ZDL service method does not have exactly one supported parameter");
        }

        Map<String, Object> target = JSONPath.get(zdl, "$.inputs['" + parameterType + "']");
        Map<String, Object> message = messages.get(0);
        String sourceType = fullyQualifiedModelType((String) message.get("x--javaType"));
        Object payloadSchema = effectivePayloadSchema(message);
        boolean safeSource = StringUtils.isNotBlank(sourceType)
                && "object".equals(JSONPath.get(payloadSchema, "$.type"));
        boolean safeTarget = target != null
                && target.get("fields") instanceof Map<?, ?>
                && !JSONPath.get(target, "$.options.inline", false);
        if (!safeSource || !safeTarget) {
            return customPlan(plannedOperation, messages, plan,
                    "the source and target types are not provably MapStruct-compatible beans");
        }

        plan.put("classification", MappingClassification.GENERATED.name());
        plan.put("generated", true);
        plan.put("targetType", layout.inboundDtosPackage + "." + parameterType);
        plan.put("sourceType", sourceType);
        plan.put("mapperMethodName", serviceMethod.get("name") + "Input");
        plannedOperation.put("adapterPlan", plan);
        addAdapterMethodNames(plannedOperation, messages);
        logPlan(plannedOperation);
        return plannedOperation;
    }

    private void addServiceDependency(Map<String, Object> plan, Map<String, Object> serviceMethod) {
        String serviceName = (String) serviceMethod.get("serviceName");
        plan.put("serviceName", serviceName);
        plan.put("serviceType", layout.inboundPackage + "." + serviceName);
        plan.put("serviceVariable", NamingUtils.asInstanceName(serviceName));
        plan.put("serviceMethodName", serviceMethod.get("name"));
    }

    private Map<String, Object> customPlan(Map<String, Object> operation, List<Map<String, Object>> messages,
            Map<String, Object> plan, String reason) {
        plan.put("reason", reason);
        operation.put("adapterPlan", plan);
        addAdapterMethodNames(operation, messages);
        logPlan(operation);
        return operation;
    }

    private void logPlan(Map<String, Object> operation) {
        log.debug("AsyncAPI adapter plan for {} in {}: {} ({})", operation.get("operationId"), currentApiName,
                JSONPath.get(operation, "$.adapterPlan.classification"),
                JSONPath.get(operation, "$.adapterPlan.reason"));
    }

    private List<Map<String, Object>> distinctMessages(Map<String, Object> operation) {
        List<Map<String, Object>> messages = AsyncAPIUtils.operationMessages(
                adapterApiModel, (String) operation.get("operationId"));
        Set<String> signatures = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> original : messages) {
            Map<String, Object> message = new LinkedHashMap<>(original);
            String javaType = fullyQualifiedModelType((String) message.get("x--javaType"));
            String simpleType = Objects.toString(message.get("x--javaTypeSimpleName"), "Object");
            String signature = Objects.toString(javaType, "") + ":" + simpleType;
            if (signatures.add(signature)) {
                message.put("x--adapterJavaType", javaType);
                message.put("x--adapterHeadersType", currentConsumerApiPackage + "."
                        + consumerServiceInterfaceName(operation) + "." + simpleType + "Headers");
                result.add(message);
            }
        }
        return result;
    }

    private void addAdapterMethodNames(Map<String, Object> operation, List<Map<String, Object>> messages) {
        for (Map<String, Object> message : messages) {
            String methodName = (String) operation.get("operationId");
            message.put("x--adapterMethodName", methodName);
        }
    }

    private List<Map<String, Object>> findServiceMethods(Map<String, Object> operation, Map<String, Object> zdl) {
        String operationId = (String) operation.get("operationId");
        String channel = (String) operation.get("x--channel");
        List<Map<String, Object>> methods = JSONPath.get(zdl, "$.services[*].methods[*]", List.of());
        List<Map<String, Object>> candidates = methods.stream()
                .filter(method -> JSONPath.get(method, "$.options.asyncapi") != null)
                .filter(this::belongsToCurrentApi)
                .toList();
        List<Map<String, Object>> byOperation = candidates.stream()
                .filter(method -> Objects.equals(operationId, method.get("name"))
                        || Objects.equals(operationId, JSONPath.get(method, "$.options.asyncapi.operationId")))
                .toList();
        if (!byOperation.isEmpty()) {
            return byOperation;
        }
        return candidates.stream()
                .filter(method -> Objects.equals(channel, JSONPath.get(method, "$.options.asyncapi.channel")))
                .toList();
    }

    private boolean belongsToCurrentApi(Map<String, Object> method) {
        String apiName = JSONPath.get(method, "$.options.asyncapi.api");
        return Objects.equals(currentApiName, apiName) || (apiName == null && asyncApiCount == 1);
    }

    private Object effectivePayloadSchema(Map<String, Object> message) {
        Object payload = message.get("payload");
        Object nestedSchema = JSONPath.get(payload, "$.schema");
        return nestedSchema != null ? nestedSchema : payload;
    }

    private List<Map<String, Object>> generatedMappings(List<Map<String, Object>> operations) {
        Map<String, Map<String, Object>> mappings = new LinkedHashMap<>();
        for (Map<String, Object> operation : operations) {
            Map<String, Object> plan = (Map<String, Object>) operation.get("adapterPlan");
            if (Boolean.TRUE.equals(plan.get("generated"))) {
                String key = plan.get("mapperMethodName") + ":" + plan.get("sourceType") + ":" + plan.get("targetType");
                mappings.putIfAbsent(key, plan);
            }
        }
        return new ArrayList<>(mappings.values());
    }

    private Map<String, Map<String, Object>> operationsByChannel(List<Map<String, Object>> operations) {
        Map<String, Map<String, Object>> channels = new LinkedHashMap<>();
        for (Map<String, Object> operation : operations) {
            String channelName = Objects.toString(operation.get("x--channel"), operation.get("operationId").toString());
            Map<String, Object> channel = channels.computeIfAbsent(channelName, this::channelModel);
            ((List<Map<String, Object>>) channel.get("operations")).add(operation);
            Map<String, Object> plan = (Map<String, Object>) operation.get("adapterPlan");
            if (StringUtils.isNotBlank((String) plan.get("serviceType"))) {
                Map<String, Map<String, Object>> dependencies =
                        (Map<String, Map<String, Object>>) channel.get("serviceDependenciesByType");
                dependencies.putIfAbsent((String) plan.get("serviceType"), Map.of(
                        "serviceType", plan.get("serviceType"),
                        "serviceVariable", plan.get("serviceVariable")));
            }
        }
        for (Map<String, Object> channel : channels.values()) {
            Map<String, Map<String, Object>> dependencies =
                    (Map<String, Map<String, Object>>) channel.remove("serviceDependenciesByType");
            channel.put("serviceDependencies", new ArrayList<>(dependencies.values()));
            channel.put("hasServiceDependencies", !dependencies.isEmpty());
            List<Map<String, Object>> channelOperations = (List<Map<String, Object>>) channel.get("operations");
            channel.put("hasGeneratedMappings", channelOperations.stream()
                    .anyMatch(operation -> Boolean.TRUE.equals(
                            JSONPath.get(operation, "$.adapterPlan.generated", false))));
            channel.put("hasConstructorDependencies", !dependencies.isEmpty());
        }
        return channels;
    }

    private Map<String, Object> channelModel(String channelName) {
        String channelClassName = NamingUtils.camelCase(channelName);
        Map<String, Object> channel = new LinkedHashMap<>();
        channel.put("channelName", channelName);
        channel.put("consumerServiceInterfaceName", consumerServicePrefix + channelClassName + consumerServiceSuffix);
        channel.put("consumerServiceInterfaceType", currentConsumerApiPackage + "."
                + consumerServicePrefix + channelClassName + consumerServiceSuffix);
        channel.put("consumerServiceName", channelClassName + consumerServiceSuffix);
        channel.put("operations", new ArrayList<Map<String, Object>>());
        channel.put("serviceDependenciesByType", new LinkedHashMap<String, Map<String, Object>>());
        return channel;
    }

    private String consumerServiceInterfaceName(Map<String, Object> operation) {
        return consumerServicePrefix + NamingUtils.camelCase((String) operation.get("x--channel"))
                + consumerServiceSuffix;
    }

    private String fullyQualifiedModelType(String javaType) {
        return StringUtils.isBlank(javaType) || javaType.contains(".") ? javaType : currentModelPackage + "." + javaType;
    }

    private Map<String, Object> apiTemplateModel(Map<String, Object> api) {
        Map<String, Object> model = new LinkedHashMap<>();
        String apiId = apiId(api);
        model.put("apiId", apiId);
        model.put("apiName", api.get("name"));
        model.put("asyncapiAdaptersModulePrefix", asyncapiAdaptersModulePrefix());
        Map<String, Object> apiConfig = JSONPath.get(api, "$.config", Map.of());

        Map<String, Object> layoutOptions = configuration != null
                ? Maps.copy(configuration.getOptions())
                : new LinkedHashMap<>();
        layoutOptions.put("apiId", apiId);
        ProjectLayout apiLayout = configuration != null
                ? configuration.layout.processedLayout(layoutOptions)
                : layout;
        Map<String, Object> apiLayoutModel = new LinkedHashMap<>(apiLayout.asMap());
        model.put("layout", apiLayoutModel);

        String explicitAdaptersPackage = Objects.toString(apiConfig.get("adaptersPackage"), null);
        if (StringUtils.isNotBlank(explicitAdaptersPackage)) {
            apiLayoutModel.put("adaptersEventsPackage", resolve(explicitAdaptersPackage, model));
        }
        model.put("consumerApiPackage", resolve(
                Objects.toString(apiConfig.getOrDefault("consumerApiPackage", consumerApiPackage)), model));
        model.put("modelPackage", resolve(
                Objects.toString(apiConfig.getOrDefault("modelPackage", modelPackage)), model));

        // Keep the planner and templates on the same resolved packages without mutating the
        // configurable templates, which may still contain {{apiId}} for the next referenced API.
        currentConsumerApiPackage = (String) model.get("consumerApiPackage");
        currentModelPackage = (String) model.get("modelPackage");
        return model;
    }

    private String apiId(Map<String, Object> api) {
        Map<String, Object> apiConfig = JSONPath.get(api, "$.config", Map.of());
        String explicit = Objects.toString(apiConfig.get("apiId"), null);
        if (StringUtils.isNotBlank(explicit)) {
            return explicit;
        }
        String name = Objects.toString(api.get("name"), "asyncapi")
                .replaceFirst("(?i)(AsyncAPI|API)$", "");
        return NamingUtils.asInstanceName(name);
    }

    private String asyncapiAdaptersModulePrefix() {
        Object prefix = additionalProperties.get("mavenModulesPrefix");
        return prefix instanceof String value && StringUtils.isNotBlank(value) ? value + "-core-impl/" : "";
    }

    private String resolve(String template, Map<String, Object> apiModel) {
        Map<String, Object> model = new HashMap<>(asConfigurationMap());
        model.putAll(apiModel);
        try {
            return getTemplateEngine().processInline(template, model);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve AsyncAPI adapter option: " + template, e);
        }
    }

    private TemplateOutput generateTemplate(Map<String, Object> contextModel, TemplateInput template,
            Map<String, Object> apiModel, Map<String, Object> extensionModel) {
        Map<String, Object> model = new HashMap<>(asConfigurationMap());
        model.putAll(templates.getDocumentedOptions());
        model.put("context", contextModel);
        model.put("zdl", contextModel.get("zdl"));
        model.put("asyncapi", adapterApiModel);
        model.putAll(apiModel);
        model.putAll(extensionModel);
        return getTemplateEngine().processTemplate(model, template);
    }

    @Override
    protected Templates configureTemplates() {
        return new Templates(templates.getTemplatesFolder());
    }
}
