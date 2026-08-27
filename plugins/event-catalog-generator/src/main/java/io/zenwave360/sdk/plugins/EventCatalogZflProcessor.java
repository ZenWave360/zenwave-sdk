package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestDomain;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.graph.ArchitectureBindingRole;
import io.zenwave360.manifest.graph.ArchitectureDiagnostic;
import io.zenwave360.manifest.graph.ArchitectureEdge;
import io.zenwave360.manifest.graph.ArchitectureEdgeKind;
import io.zenwave360.manifest.graph.ArchitectureGraph;
import io.zenwave360.manifest.graph.ArchitectureGraphResult;
import io.zenwave360.manifest.graph.ArchitectureNode;
import io.zenwave360.manifest.graph.ArchitectureNodeKind;
import io.zenwave360.manifest.graph.ArchitectureOperationBinding;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowActorFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowCustomFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowNextStepFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowStepFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure EventCatalog projection of manifest-graph ZFL semantics. */
public class EventCatalogZflProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        ArchitectureGraphResult result = (ArchitectureGraphResult) contextModel.get("architectureGraph");
        if (manifest == null || eventCatalog == null || result == null) return contextModel;

        result.getDiagnostics().forEach(this::logDiagnostic);
        ArchitectureGraph graph = result.getGraph();
        Map<String, Integer> idsByDomain = new LinkedHashMap<>();
        graph.getNodes().stream()
                .filter(node -> node.getKind() == ArchitectureNodeKind.ZFL_FLOW)
                .sorted(nodeOrder())
                .forEach(flow -> projectFlow(manifest, eventCatalog, graph, flow, idsByDomain));
        return contextModel;
    }

    private void projectFlow(ZenWaveManifest manifest, EventCatalogModel eventCatalog,
                             ArchitectureGraph graph, ArchitectureNode flow,
                             Map<String, Integer> idsByDomain) {
        ManifestDomain domain = owningDomain(manifest, graph, flow);
        if (domain == null) {
            log.warn("ZFL flow '{}' has no manifest domain owner and cannot be written", flow.getLabel());
            return;
        }
        ArchitectureNode artifact = graph.node(flow.getOwnerId());
        String version = artifact != null ? artifact.getAttributes().get("version") : null;
        if (version == null || version.isBlank()) version = "0.0.1";

        String baseId = slug(flow.getLabel());
        String domainScopedId = domain.getId() + "/" + baseId;
        int occurrence = idsByDomain.merge(domainScopedId, 1, Integer::sum);
        String flowId = occurrence == 1 ? baseId : baseId + "-" + occurrence;
        FlowProjection projection = projectSteps(manifest, eventCatalog, graph, flow);

        Map<String, Object> flowData = new LinkedHashMap<>();
        flowData.put("id", flowId);
        flowData.put("name", humanize(flow.getLabel()));
        flowData.put("summary", valueOr(flow.getDescription(), "Business flow generated from " + flow.getLabel() + "."));
        if (flow.getDescription() != null) flowData.put("description", flow.getDescription());
        flowData.put("version", version);
        flowData.put("domainId", domain.getId());
        flowData.put("steps", projection.steps());
        eventCatalog.addFlow(flowData);

        Map<String, Object> pointer = pointer(flowId, version, null);
        addUniqueMap(eventCatalog.domainData(domain), "flows", pointer);
        projection.services().forEach(service -> addUniqueMap(eventCatalog.serviceData(service), "flows", pointer));
    }

    private FlowProjection projectSteps(ZenWaveManifest manifest, EventCatalogModel eventCatalog,
                                        ArchitectureGraph graph, ArchitectureNode flow) {
        List<ArchitectureNode> direct = graph.getNodes().stream()
                .filter(node -> flow.getId().equals(node.getOwnerId()))
                .toList();
        List<ArchitectureNode> operations = direct.stream()
                .filter(node -> node.getKind() == ArchitectureNodeKind.ZFL_OPERATION).toList();
        List<ArchitectureNode> occurrences = operations.stream()
                .flatMap(operation -> graph.operationOccurrences(operation.getId()).stream())
                .sorted(nodeOrder()).toList();
        List<ArchitectureNode> starts = direct.stream()
                .filter(node -> node.getKind() == ArchitectureNodeKind.ZFL_STEP)
                .filter(node -> "start".equals(node.getAttributes().get("role")))
                .sorted(nodeOrder()).toList();
        List<ArchitectureNode> events = direct.stream()
                .filter(node -> node.getKind() == ArchitectureNodeKind.ZFL_EVENT)
                .sorted(nodeOrder()).toList();
        List<ArchitectureNode> outcomes = graph.flowOutcomes(flow.getId()).stream().sorted(nodeOrder()).toList();

        Set<String> semanticIds = new LinkedHashSet<>();
        direct.forEach(node -> semanticIds.add(node.getId()));
        occurrences.forEach(node -> semanticIds.add(node.getId()));
        List<ArchitectureEdge> edges = graph.getEdges().stream()
                .filter(edge -> semanticIds.contains(edge.getSource()) && semanticIds.contains(edge.getTarget()))
                .filter(edge -> Set.of(
                        ArchitectureEdgeKind.TRIGGERS, ArchitectureEdgeKind.EMITS,
                        ArchitectureEdgeKind.RESPONDS, ArchitectureEdgeKind.INVOKES,
                        ArchitectureEdgeKind.COMPENSATES, ArchitectureEdgeKind.RESULTS_IN).contains(edge.getKind()))
                .sorted(Comparator.comparing(ArchitectureEdge::getId)).toList();

        List<ProjectedStep> projected = new ArrayList<>();
        Map<String, StepGroup> groups = new LinkedHashMap<>();
        Set<ManifestService> services = new LinkedHashSet<>();

        for (ArchitectureNode start : starts) {
            ProjectedStep step = startStep(start);
            projected.add(step);
            groups.put(start.getId(), new StepGroup(step, step));
        }
        for (ArchitectureNode event : events) {
            ArchitectureEdge startTrigger = graph.incoming(event.getId(), ArchitectureEdgeKind.TRIGGERS).stream()
                    .filter(edge -> groups.containsKey(edge.getSource())).findFirst().orElse(null);
            if (startTrigger != null) {
                groups.put(event.getId(), groups.get(startTrigger.getSource()));
            } else {
                ProjectedStep step = eventStep(manifest, eventCatalog, graph, flow, event);
                projected.add(step);
                groups.put(event.getId(), new StepGroup(step, step));
            }
        }
        for (ArchitectureNode occurrence : occurrences) {
            ArchitectureNode operation = occurrenceOperation(graph, occurrence);
            StepGroup group = operationGroup(
                    manifest, eventCatalog, graph, flow, occurrence, operation, occurrence.getId(), services, projected);
            groups.put(occurrence.getId(), group);
            groups.putIfAbsent(operation.getId(), group);
        }
        for (ArchitectureNode outcome : outcomes) {
            String color = Boolean.parseBoolean(outcome.getAttributes().get("failure")) ? "red" : "green";
            ProjectedStep step = ProjectedStep.custom(
                    outcome.getId(), humanize(outcome.getLabel()), outcome.getDescription(), "outcome", color);
            projected.add(step);
            groups.put(outcome.getId(), new StepGroup(step, step));
        }

        for (ArchitectureEdge edge : edges) {
            StepGroup source = groups.get(edge.getSource());
            StepGroup target = groups.get(edge.getTarget());
            if (edge.getKind() == ArchitectureEdgeKind.INVOKES) {
                ArchitectureNode targetOperation = graph.node(edge.getTarget());
                if (targetOperation != null && targetOperation.getKind() == ArchitectureNodeKind.ZFL_OPERATION) {
                    target = operationGroup(manifest, eventCatalog, graph, flow, targetOperation,
                            targetOperation, edge.getId(), services, projected);
                }
            }
            if (source == null || target == null) continue;
            String label = edgeLabel(edge);
            if (edge.getKind() == ArchitectureEdgeKind.COMPENSATES) label = valueOr(label, "compensates");
            if (edge.getKind() == ArchitectureEdgeKind.RESPONDS) label = valueOr(label, "response");
            connect(source.exit(), target.entry(), label);
        }

        return new FlowProjection(projected.stream().map(ProjectedStep::toFrontmatter).toList(), services);
    }

    private ProjectedStep startStep(ArchitectureNode node) {
        String actor = node.getAttributes().get("actor");
        String timer = node.getAttributes().get("timer");
        if (actor != null && !actor.isBlank()) {
            return ProjectedStep.actor(node.getId(), humanize(node.getLabel()), timerSummary(timer), actor);
        }
        if (timer != null && !timer.isBlank()) {
            return ProjectedStep.custom(node.getId(), humanize(node.getLabel()), "Scheduled: " + timer, "timer", "blue");
        }
        return ProjectedStep.custom(node.getId(), humanize(node.getLabel()), node.getDescription(), "start", null);
    }

    private ProjectedStep eventStep(ZenWaveManifest manifest, EventCatalogModel eventCatalog,
                                    ArchitectureGraph graph, ArchitectureNode flow, ArchitectureNode event) {
        CatalogResource resource = matchEvent(eventCatalog, manifest, graph, event);
        if (resource != null) {
            return ProjectedStep.message(event.getId(), resource.name(), resource.summary(), resource.pointer());
        }
        warn(flow, event, "event", event.getLabel());
        String color = Boolean.parseBoolean(event.getAttributes().get("isError")) ? "red" : null;
        return ProjectedStep.custom(event.getId(), humanize(event.getLabel()), event.getDescription(), "event", color);
    }

    private StepGroup operationGroup(ZenWaveManifest manifest, EventCatalogModel eventCatalog,
                                     ArchitectureGraph graph, ArchitectureNode flow,
                                     ArchitectureNode visualIdentity, ArchitectureNode operation,
                                     String stepId, Set<ManifestService> participatingServices,
                                     List<ProjectedStep> projected) {
        ManifestService service = resolveService(manifest, graph, operation);
        ArchitectureNode method = graph.resolvedMethod(operation.getId());
        CatalogResource resource = service != null && method != null
                ? matchOperation(eventCatalog, service, method.getId(), operation.getLabel()) : null;
        String summary = valueOr(visualIdentity.getDescription(), operation.getDescription());

        if (service == null) {
            warn(flow, visualIdentity, "service", visualIdentity.getAttributes().get("servicePath"));
            ProjectedStep unresolved = ProjectedStep.custom(
                    stepId, humanize(operation.getLabel()), summary, "unresolved-operation", "red");
            projected.add(unresolved);
            return new StepGroup(unresolved, unresolved);
        }

        participatingServices.add(service);
        ResourcePointerFrontmatter servicePointer = new ResourcePointerFrontmatter(
                eventCatalog.catalogServiceId(service), eventCatalog.effectiveServiceVersion(service), null);
        if (resource == null) {
            ProjectedStep serviceStep = ProjectedStep.service(
                    stepId + ":service", humanize(operation.getLabel()), summary, servicePointer);
            ProjectedStep internal = ProjectedStep.custom(
                    stepId, humanize(operation.getLabel()), summary, "operation", "blue");
            projected.add(serviceStep);
            projected.add(internal);
            connect(serviceStep, internal, null);
            return new StepGroup(serviceStep, internal);
        }

        ProjectedStep serviceStep = ProjectedStep.service(
                stepId + ":service", humanize(operation.getLabel()), summary, servicePointer);
        ProjectedStep messageStep = ProjectedStep.message(
                stepId, resource.name(), valueOr(summary, resource.summary()), resource.pointer());
        projected.add(serviceStep);
        projected.add(messageStep);
        connect(serviceStep, messageStep, null);
        return new StepGroup(serviceStep, messageStep);
    }

    private ArchitectureNode occurrenceOperation(ArchitectureGraph graph, ArchitectureNode occurrence) {
        return graph.outgoing(occurrence.getId(), ArchitectureEdgeKind.OCCURRENCE_OF).stream()
                .map(edge -> graph.node(edge.getTarget()))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow();
    }

    private ManifestService resolveService(ZenWaveManifest manifest, ArchitectureGraph graph,
                                           ArchitectureNode operation) {
        ArchitectureNode method = graph.resolvedMethod(operation.getId());
        if (method == null) return null;
        ArchitectureNode service = graph.owningService(method.getId());
        if (service == null) return null;
        String serviceRef = service.getAttributes().get("serviceRef");
        return serviceRef != null ? manifest.findService(serviceRef) : null;
    }

    private CatalogResource matchOperation(EventCatalogModel eventCatalog, ManifestService service,
                                           String graphMethodId, String displayName) {
        Map<String, Object> operation = eventCatalog.operation(service, graphMethodId);
        if (operation == null || operation.get("resourceId") == null || "conflict".equals(operation.get("visibility"))) {
            return null;
        }
        return new CatalogResource(
                string(operation.get("resourceId")),
                valueOr(string(operation.get("resourceVersion")), eventCatalog.effectiveServiceVersion(service)),
                valueOr(string(operation.get("resourceType")), "command"),
                valueOr(string(operation.get("resourceName")), humanize(displayName)),
                string(operation.get("resourceSummary")));
    }

    private CatalogResource matchEvent(EventCatalogModel eventCatalog, ZenWaveManifest manifest,
                                       ArchitectureGraph graph, ArchitectureNode event) {
        for (ArchitectureEdge resolution : graph.outgoing(event.getId(), ArchitectureEdgeKind.RESOLVES_TO)) {
            ArchitectureNode zdlEvent = graph.node(resolution.getTarget());
            if (zdlEvent == null || zdlEvent.getKind() != ArchitectureNodeKind.ZDL_EVENT) continue;
            ManifestService service = manifestService(manifest, graph.owningService(zdlEvent.getId()));
            if (service == null) continue;
            for (ArchitectureEdge bindingEdge : graph.operationBindings(zdlEvent.getId())) {
                ArchitectureOperationBinding binding = ArchitectureOperationBinding.from(bindingEdge);
                if (binding == null || binding.getRole() == ArchitectureBindingRole.INVOCATION) continue;
                CatalogResource resource = resourceByGraphId(eventCatalog, service, bindingEdge.getTarget(), "event");
                if (resource != null) return resource;
            }
        }
        return null;
    }

    private CatalogResource resourceByGraphId(EventCatalogModel eventCatalog, ManifestService service,
                                              String graphResourceId, String expectedType) {
        Map<String, Object> data = eventCatalog.serviceData(service);
        for (String key : List.of("_events", "_commands", "_queries")) {
            Object raw = data.get(key);
            if (!(raw instanceof List<?> resources)) continue;
            for (Object item : resources) {
                if (!(item instanceof Map<?, ?> resource)) continue;
                if (!graphResourceId.equals(string(resource.get("_graphResourceNodeId")))) continue;
                String id = string(resource.get("id"));
                if (id == null) continue;
                return new CatalogResource(
                        id,
                        valueOr(string(resource.get("version")), eventCatalog.effectiveServiceVersion(service)),
                        expectedType,
                        valueOr(string(resource.get("name")), humanize(id.substring(id.lastIndexOf('.') + 1))),
                        string(resource.get("summary")));
            }
        }
        return null;
    }

    private ManifestDomain owningDomain(ZenWaveManifest manifest, ArchitectureGraph graph, ArchitectureNode flow) {
        ArchitectureNode artifact = graph.node(flow.getOwnerId());
        ArchitectureNode owner = artifact != null ? graph.node(artifact.getOwnerId()) : null;
        if (owner == null || owner.getKind() != ArchitectureNodeKind.DOMAIN) return null;
        String key = owner.getAttributes().get("key");
        return manifest.getDomains().stream().filter(domain -> domain.getKey().equals(key)).findFirst().orElse(null);
    }

    private ManifestService manifestService(ZenWaveManifest manifest, ArchitectureNode serviceNode) {
        if (serviceNode == null) return null;
        String serviceRef = serviceNode.getAttributes().get("serviceRef");
        return serviceRef != null ? manifest.findService(serviceRef) : null;
    }

    private Comparator<ArchitectureNode> nodeOrder() {
        return Comparator
                .comparingInt((ArchitectureNode node) -> node.getSource() != null && node.getSource().getLine() != null
                        ? node.getSource().getLine() : Integer.MAX_VALUE)
                .thenComparing(ArchitectureNode::getId);
    }

    private String edgeLabel(ArchitectureEdge edge) {
        return valueOr(edge.getAttributes().get("outcome"),
                valueOr(edge.getAttributes().get("condition"), edge.getLabel()));
    }

    private void connect(ProjectedStep source, ProjectedStep target, String label) {
        if (source == null || target == null || Objects.equals(source.id(), target.id())) return;
        source.next.putIfAbsent(target.id() + "|" + valueOr(label, ""),
                new FlowNextStepFrontmatter(target.id(), label));
    }

    private void logDiagnostic(ArchitectureDiagnostic diagnostic) {
        String location = diagnostic.getSource() != null ? diagnostic.getSource().getUri() : null;
        log.warn("Manifest graph [{}]{}: {}", diagnostic.getCode(),
                location != null ? " at " + location : "", diagnostic.getMessage());
    }

    private void warn(ArchitectureNode flow, ArchitectureNode node, String kind, String reference) {
        log.warn("ZFL flow '{}' step '{}' has unresolved {} '{}'", flow.getLabel(), node.getLabel(), kind,
                valueOr(reference, node.getLabel()));
    }

    @SuppressWarnings("unchecked")
    private void addUniqueMap(Map<String, Object> owner, String key, Map<String, Object> value) {
        List<Map<String, Object>> values =
                (List<Map<String, Object>>) owner.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (values.stream().noneMatch(existing -> Objects.equals(existing.get("id"), value.get("id")))) {
            values.add(new LinkedHashMap<>(value));
        }
    }

    private Map<String, Object> pointer(String id, String version, String type) {
        Map<String, Object> pointer = new LinkedHashMap<>();
        pointer.put("id", id);
        pointer.put("version", version);
        if (type != null) pointer.put("type", type);
        return pointer;
    }

    private String timerSummary(String timer) {
        return timer != null && !timer.isBlank() ? "Scheduled: " + timer : null;
    }

    private String slug(String value) {
        if (value == null || value.isBlank()) return "step";
        return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) return value;
        String spaced = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('-', ' ').replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String string(Object value) { return value != null ? value.toString() : null; }

    private String valueOr(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private record FlowProjection(List<FlowStepFrontmatter> steps, Set<ManifestService> services) {}
    private record StepGroup(ProjectedStep entry, ProjectedStep exit) {}
    private record CatalogResource(String id, String version, String type, String name, String summary) {
        ResourcePointerFrontmatter pointer() { return new ResourcePointerFrontmatter(id, version, type); }
    }

    private static final class ProjectedStep {
        private final String id;
        private final String type;
        private final String title;
        private final String summary;
        private final ResourcePointerFrontmatter message;
        private final ResourcePointerFrontmatter service;
        private final FlowActorFrontmatter actor;
        private final FlowCustomFrontmatter custom;
        private final Map<String, FlowNextStepFrontmatter> next = new LinkedHashMap<>();

        private ProjectedStep(String id, String type, String title, String summary,
                              ResourcePointerFrontmatter message, ResourcePointerFrontmatter service,
                              FlowActorFrontmatter actor, FlowCustomFrontmatter custom) {
            this.id = id; this.type = type; this.title = title; this.summary = summary;
            this.message = message; this.service = service; this.actor = actor; this.custom = custom;
        }

        static ProjectedStep actor(String id, String title, String summary, String actor) {
            return new ProjectedStep(id, "actor", title, summary, null, null,
                    new FlowActorFrontmatter(actor, summary), null);
        }

        static ProjectedStep service(String id, String title, String summary, ResourcePointerFrontmatter service) {
            return new ProjectedStep(id, "node", title, summary, null, service, null, null);
        }

        static ProjectedStep message(String id, String title, String summary, ResourcePointerFrontmatter message) {
            return new ProjectedStep(id, "message", title, summary, message, null, null, null);
        }

        static ProjectedStep custom(String id, String title, String summary, String customType, String color) {
            return new ProjectedStep(id, "node", title, summary, null, null, null,
                    new FlowCustomFrontmatter(title, null, customType, summary, null, color, null, null, null));
        }

        String id() { return id; }

        FlowStepFrontmatter toFrontmatter() {
            List<FlowNextStepFrontmatter> links = List.copyOf(next.values());
            FlowNextStepFrontmatter nextStep = links.size() == 1 ? links.get(0) : null;
            List<FlowNextStepFrontmatter> nextSteps = links.size() > 1 ? links : null;
            return new FlowStepFrontmatter(
                    id, type, title, summary, message, null, service, null, null, null,
                    actor, custom, null, nextStep, nextSteps);
        }
    }
}
