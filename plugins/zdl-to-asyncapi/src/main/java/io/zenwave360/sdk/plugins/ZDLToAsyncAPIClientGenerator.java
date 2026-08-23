package io.zenwave360.sdk.plugins;

import org.apache.commons.lang3.Strings;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;
import static io.zenwave360.sdk.utils.NamingUtils.humanReadable;
import static io.zenwave360.sdk.utils.NamingUtils.kebabCase;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public class ZDLToAsyncAPIClientGenerator extends AbstractZDLGenerator {

    public String sourceProperty = "zdl";

    @DocumentedOption(description = "Target AsyncAPI client file")
    public String targetFile = "./asyncapi-client.yml";

    @DocumentedOption(description = "AsyncAPI document id")
    public String id;

    @DocumentedOption(description = "AsyncAPI document title")
    public String title;

    @DocumentedOption(description = "AsyncAPI document version")
    public String version = "1.0.0";

    private final TemplateInput template = new TemplateInput(
            "io/zenwave360/sdk/plugins/ZDLToAsyncAPIClientGenerator/ZDLToAsyncAPIClient.yml",
            "{{targetFile}}")
            .withMimeType(OutputFormatType.YAML);

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        var generatedProjectFiles = new GeneratedProjectFiles();
        Map<String, Object> model = getModel(contextModel);
        Map<String, Object> clientModel = buildClientModel(model);
        generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, template, clientModel));
        return generatedProjectFiles;
    }

    protected Map<String, Object> getModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    private Map<String, Object> buildClientModel(Map<String, Object> model) {
        var consumedChannels = findConsumedChannels(model);
        var channelCounts = consumedChannels.stream()
                .collect(Collectors.groupingBy(ConsumedChannel::channelKey, LinkedHashMap::new, Collectors.counting()));

        var channels = new LinkedHashMap<String, Object>();
        var operations = new LinkedHashMap<String, Object>();
        for (ConsumedChannel consumedChannel : consumedChannels) {
            putChannel(channels, consumedChannel);
            putOperation(operations, consumedChannel, channelCounts.get(consumedChannel.channelKey()) > 1);
        }

        var serviceTitle = serviceTitle(model);
        var serviceName = serviceName(model);
        var asyncApiServiceName = Strings.CS.removeEnd(serviceName, "Service");
        return Maps.of(
                "id",
                firstNonNull(trimToNull(id), "urn:arcadiaeditions:asyncapi:" + kebabCase(asyncApiServiceName) + ":client"),
                "title",
                firstNonNull(trimToNull(title), "AsyncAPI client for " + serviceTitle),
                "version",
                version,
                "description",
                "Consumed AsyncAPI channels for " + serviceTitle + ".",
                "channels",
                channels,
                "operations",
                operations);
    }

    private List<ConsumedChannel> findConsumedChannels(Map<String, Object> model) {
        Map<String, Map<String, Object>> apis = JSONPath.get(model, "$.apis", Map.of());
        List<Map<String, Object>> methods = JSONPath.get(model, "$.services[*].methods[*][?(@.options.asyncapi)]", Collections.emptyList());
        validateApiReferences(apis, methods);
        return apis.values().stream()
                .filter(this::isAsyncApiClient)
                .flatMap(api -> methods.stream()
                        .filter(method -> Objects.equals(api.get("name"), JSONPath.get(method, "$.options.asyncapi.api")))
                        .map(method -> toConsumedChannel(api, method)))
                .toList();
    }

    private void validateApiReferences(Map<String, Map<String, Object>> apis, List<Map<String, Object>> methods) {
        for (Map<String, Object> method : methods) {
            var apiName = trimToNull(JSONPath.get(method, "$.options.asyncapi.api", (String) null));
            if (apiName != null && !apis.containsKey(apiName)) {
                throw new IllegalArgumentException("Method '" + method.get("name") + "' references undeclared asyncapi API: " + apiName);
            }
            if (apiName != null && trimToNull(JSONPath.get(method, "$.options.asyncapi.channel", (String) null)) == null) {
                throw new IllegalArgumentException("Missing asyncapi channel for method: " + method.get("name"));
            }
        }
    }

    private boolean isAsyncApiClient(Map<String, Object> api) {
        return Objects.equals("asyncapi", api.get("type")) && Objects.equals("client", api.get("role"));
    }

    private ConsumedChannel toConsumedChannel(Map<String, Object> api, Map<String, Object> method) {
        var channelName = trimToNull(JSONPath.get(method, "$.options.asyncapi.channel", (String) null));
        var apiName = (String) api.get("name");
        var apiUri = trimToNull(firstNonNull((String) api.get("uri"), JSONPath.get(api, "$.config.uri", (String) null)));
        if (apiUri == null) {
            throw new IllegalArgumentException("Missing uri for asyncapi client API: " + apiName);
        }
        validateUri(apiName, apiUri);
        return new ConsumedChannel(apiName, apiUri, channelName, (String) method.get("name"));
    }

    private void validateUri(String apiName, String apiUri) {
        try {
            new URI(apiUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed uri for asyncapi client API '" + apiName + "': " + apiUri, e);
        }
    }

    private void putChannel(Map<String, Object> channels, ConsumedChannel consumedChannel) {
        var ref = consumedChannel.ref();
        var existingChannel = (Map<String, Object>) channels.get(consumedChannel.channelName);
        if (existingChannel != null && !Objects.equals(ref, existingChannel.get("ref"))) {
            throw new IllegalArgumentException("Channel '" + consumedChannel.channelName + "' is consumed from multiple API URIs");
        }
        channels.putIfAbsent(consumedChannel.channelName, Maps.of("ref", ref));
    }

    private void putOperation(Map<String, Object> operations, ConsumedChannel consumedChannel, boolean duplicateChannel) {
        var operationName = duplicateChannel
                ? "on" + Strings.CS.removeEnd(asJavaTypeName(consumedChannel.apiName), "Api") + asJavaTypeName(consumedChannel.methodName)
                : "on" + Strings.CS.removeEnd(asJavaTypeName(consumedChannel.channelName), "Channel");
        if (operations.containsKey(operationName)) {
            throw new IllegalArgumentException("Duplicate AsyncAPI client operation name: " + operationName);
        }
        operations.put(
                operationName,
                Maps.of("action", "receive", "channel", consumedChannel.channelName));
    }

    private String serviceName(Map<String, Object> model) {
        List<String> serviceNames = JSONPath.get(model, "$.services[*].name", List.of());
        return serviceNames.isEmpty() ? basePackage : serviceNames.get(0);
    }

    private String serviceTitle(Map<String, Object> model) {
        var configTitle = trimToNull(JSONPath.get(model, "$.config.title", (String) null));
        if (configTitle != null) {
            return configTitle;
        }
        return asTitle(Strings.CS.removeEnd(serviceName(model), "Service"));
    }

    private String asTitle(String value) {
        var words = humanReadable(value).split(" ");
        for (int i = 0; i < words.length; i++) {
            words[i] = asJavaTypeName(words[i]);
        }
        return String.join(" ", words);
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> clientModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("clientModel", clientModel);
        return getTemplateEngine().processTemplate(model, template);
    }

    private record ConsumedChannel(String apiName, String apiUri, String channelName, String methodName) {

        String channelKey() {
            return apiName + ":" + channelName;
        }

        String ref() {
            return apiUri + "#/channels/" + channelName;
        }
    }
}
