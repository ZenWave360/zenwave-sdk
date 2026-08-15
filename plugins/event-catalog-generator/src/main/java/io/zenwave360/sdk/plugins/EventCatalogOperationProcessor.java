package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.graph.ArchitectureBindingRole;
import io.zenwave360.manifest.graph.ArchitectureEdge;
import io.zenwave360.manifest.graph.ArchitectureGraph;
import io.zenwave360.manifest.graph.ArchitectureGraphResult;
import io.zenwave360.manifest.graph.ArchitectureNode;
import io.zenwave360.manifest.graph.ArchitectureNodeKind;
import io.zenwave360.manifest.graph.ArchitectureOperationBinding;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reconciles catalog resources exclusively through validated manifest-graph identities. */
public class EventCatalogOperationProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Publish unbound ZDL operations as synthesized EventCatalog command/query pages.")
    public Boolean publishInternalOperations;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        ArchitectureGraphResult graphResult = (ArchitectureGraphResult) contextModel.get("architectureGraph");
        if (manifest == null || eventCatalog == null || graphResult == null) return contextModel;

        ArchitectureGraph graph = graphResult.getGraph();
        for (ManifestService service : manifest.getServices()) {
            reconcile(service, eventCatalog, graph);
        }
        return contextModel;
    }

    private void reconcile(ManifestService service, EventCatalogModel eventCatalog, ArchitectureGraph graph) {
        Map<String, Object> serviceData = eventCatalog.serviceData(service);
        List<Map<String, Object>> asyncCommands = mutableMaps(serviceData.get("_commands"));
        List<Map<String, Object>> restCommands = mutableMaps(serviceData.get("_restCommands"));
        List<Map<String, Object>> restQueries = mutableMaps(serviceData.get("_queries"));
        List<Map<String, Object>> allCandidates = new ArrayList<>();
        allCandidates.addAll(asyncCommands);
        allCandidates.addAll(restCommands);
        allCandidates.addAll(restQueries);
        Map<String, Map<String, Object>> candidatesByGraphId = allCandidates.stream()
                .filter(candidate -> candidate.get("_graphResourceNodeId") != null)
                .collect(Collectors.toMap(
                        candidate -> candidate.get("_graphResourceNodeId").toString(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        List<ArchitectureNode> methods = graph.getNodes().stream()
                .filter(node -> node.getKind() == ArchitectureNodeKind.ZDL_METHOD)
                .filter(node -> {
                    ArchitectureNode owner = graph.owningService(node.getId());
                    return owner != null && service.getServiceRef().equals(owner.getAttributes().get("serviceRef"));
                })
                .sorted(Comparator.comparing(ArchitectureNode::getId))
                .toList();

        Set<String> consumedGraphResources = new LinkedHashSet<>();
        List<Map<String, Object>> operations = new ArrayList<>();
        List<Map<String, Object>> reconciledCommands = new ArrayList<>();
        List<Map<String, Object>> reconciledQueries = new ArrayList<>();

        for (ArchitectureNode method : methods) {
            List<ArchitectureEdge> bindingEdges = graph.operationBindings(
                    method.getId(), ArchitectureBindingRole.INVOCATION.getWireValue());
            List<String> unresolvedPointers = bindingEdges.stream()
                    .filter(edge -> ArchitectureOperationBinding.from(edge) != null)
                    .filter(edge -> !validCatalogPointer(
                            candidatesByGraphId.get(edge.getTarget()), service, eventCatalog))
                    .map(ArchitectureEdge::getTarget)
                    .toList();
            List<BoundCandidate> bindings = bindingEdges.stream()
                    .map(edge -> new BoundCandidate(
                            ArchitectureOperationBinding.from(edge),
                            candidatesByGraphId.get(edge.getTarget())))
                    .filter(bound -> bound.binding() != null &&
                            validCatalogPointer(bound.candidate(), service, eventCatalog))
                    .toList();
            bindings.forEach(bound -> consumedGraphResources.add(bound.binding().getEdge().getTarget()));

            Set<String> intents = bindings.stream()
                    .map(bound -> bound.binding().getMessageKind().getWireValue())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            String modeledIntent = valueOr(method.getAttributes().get("intent"),
                    intents.contains("query") ? "query" : "command");
            Map<String, Object> registry = new LinkedHashMap<>();
            registry.put("id", method.getId());
            registry.put("graphOperationId", method.getId());
            registry.put("name", method.getLabel());
            registry.put("intent", modeledIntent);
            registry.put("bindings", bindings.stream().map(this::bindingMap).toList());
            if (!unresolvedPointers.isEmpty()) {
                registry.put("diagnostic", "unresolved-catalog-resource-pointer");
                log.warn("[unresolved-catalog-resource-pointer] Operation '{}' in service '{}' cannot resolve " +
                                "bound EventCatalog resources {} at version '{}'",
                        method.getLabel(), service.getServiceRef(), unresolvedPointers,
                        eventCatalog.effectiveServiceVersion(service));
            }

            if (intents.size() > 1 || (!intents.isEmpty() && !intents.contains(modeledIntent))) {
                registry.put("visibility", "conflict");
                registry.put("diagnostic", "conflicting-operation-intent");
                operations.add(registry);
                log.warn("Operation '{}' in service '{}' has conflicting invocation binding intents {}",
                        method.getLabel(), service.getServiceRef(), intents);
                continue;
            }

            Map<String, Object> resource = bindings.isEmpty() ? null
                    : modeledResource(method, service, eventCatalog, bindings, modeledIntent);
            if (resource == null && Boolean.TRUE.equals(publishInternalOperations)) {
                resource = synthesizedResource(method, service, eventCatalog, modeledIntent);
            }
            if (resource == null) {
                registry.put("visibility", "internal");
            } else {
                bindRegistryEntry(registry, resource, modeledIntent);
                if ("query".equals(modeledIntent)) addUnique(reconciledQueries, resource);
                else addUnique(reconciledCommands, resource);
            }
            operations.add(registry);
        }

        for (Map<String, Object> candidate : asyncCommands) {
            if (!consumedGraphResources.contains(string(candidate.get("_graphResourceNodeId")))) {
                addUnique(reconciledCommands, candidate);
            }
        }
        for (Map<String, Object> candidate : restCommands) {
            if (!consumedGraphResources.contains(string(candidate.get("_graphResourceNodeId")))) {
                addUnique(reconciledCommands, candidate);
            }
        }
        for (Map<String, Object> candidate : restQueries) {
            if (!consumedGraphResources.contains(string(candidate.get("_graphResourceNodeId")))) {
                addUnique(reconciledQueries, candidate);
            }
        }

        serviceData.put("_operations", operations);
        serviceData.put("_commands", reconciledCommands);
        serviceData.put("_queries", reconciledQueries);
        serviceData.remove("_restCommands");
    }

    private Map<String, Object> modeledResource(ArchitectureNode method, ManifestService service,
                                                 EventCatalogModel eventCatalog,
                                                 List<BoundCandidate> bindings, String intent) {
        Map<String, Object> primary = bindings.stream()
                .map(BoundCandidate::candidate)
                .filter(Objects::nonNull)
                .findFirst().map(LinkedHashMap::new).orElse(null);
        if (primary == null) return null;
        primary.put("id", logicalResourceId(service, eventCatalog, method.getLabel()));
        primary.put("name", valueOr(string(primary.get("name")), humanize(method.getLabel())));
        primary.put("_intent", intent);
        primary.put("_bindingTransports", bindings.stream()
                .map(bound -> bound.binding().getTransport().getWireValue()).distinct().toList());
        bindings.stream().map(BoundCandidate::candidate).forEach(candidate -> {
            copyIfAbsent(candidate, primary, "schemaPath");
            copyIfAbsent(candidate, primary, "operation");
            copyIfAbsent(candidate, primary, "summary");
        });
        return primary;
    }

    private Map<String, Object> synthesizedResource(ArchitectureNode method, ManifestService service,
                                                     EventCatalogModel eventCatalog, String intent) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("id", logicalResourceId(service, eventCatalog, method.getLabel()));
        resource.put("name", humanize(method.getLabel()));
        resource.put("summary", method.getDescription());
        resource.put("version", eventCatalog.effectiveServiceVersion(service));
        resource.put("_syntheticInternal", true);
        resource.put("_bindingTransports", List.of());
        resource.put("_intent", intent);
        return resource;
    }

    private Map<String, Object> bindingMap(BoundCandidate bound) {
        ArchitectureOperationBinding binding = bound.binding();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("graphResourceNodeId", binding.getEdge().getTarget());
        result.put("transport", binding.getTransport().getWireValue());
        result.put("role", binding.getRole().getWireValue());
        result.put("messageKind", binding.getMessageKind().getWireValue());
        result.put("direction", binding.getDirection().getWireValue());
        if (bound.candidate() != null) {
            result.put("resourceId", bound.candidate().get("id"));
            result.put("resourceVersion", bound.candidate().get("version"));
        }
        putIfPresent(result, "operationId", binding.getOperationId());
        putIfPresent(result, "method", binding.getMethod());
        putIfPresent(result, "path", binding.getPath());
        putIfPresent(result, "channelKey", binding.getChannelKey());
        putIfPresent(result, "address", binding.getAddress());
        return result;
    }

    private void bindRegistryEntry(Map<String, Object> operation, Map<String, Object> resource, String intent) {
        operation.put("visibility", Boolean.TRUE.equals(resource.get("_syntheticInternal")) ? "internal" : "exposed");
        operation.put("resourceId", resource.get("id"));
        operation.put("resourceVersion", resource.get("version"));
        operation.put("resourceType", intent);
        operation.put("resourceName", resource.get("name"));
        operation.put("resourceSummary", resource.get("summary"));
    }

    private boolean validCatalogPointer(Map<String, Object> candidate, ManifestService service,
                                        EventCatalogModel eventCatalog) {
        return candidate != null && candidate.get("id") != null &&
                Objects.equals(eventCatalog.effectiveServiceVersion(service), string(candidate.get("version")));
    }

    private String logicalResourceId(ManifestService service, EventCatalogModel model, String methodName) {
        return model.catalogServiceId(service) + "." + slug(methodName);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mutableMaps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(value instanceof List<?> list)) return result;
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) result.add(new LinkedHashMap<>((Map<String, Object>) map));
        }
        return result;
    }

    private void addUnique(List<Map<String, Object>> values, Map<String, Object> value) {
        if (value == null || value.get("id") == null) return;
        if (values.stream().noneMatch(existing -> Objects.equals(existing.get("id"), value.get("id")))) {
            values.add(new LinkedHashMap<>(value));
        }
    }

    private void copyIfAbsent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source != null && target.get(key) == null && source.get(key) != null) target.put(key, source.get(key));
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private String slug(String value) {
        if (value == null || value.isBlank()) return "operation";
        return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "").toLowerCase(Locale.ROOT);
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) return value;
        String spaced = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String valueOr(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String string(Object value) {
        return value != null ? value.toString() : null;
    }

    private record BoundCandidate(ArchitectureOperationBinding binding, Map<String, Object> candidate) {}
}
