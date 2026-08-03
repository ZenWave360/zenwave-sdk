package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestDomain;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ManifestSubdomain;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.plugins.frontmatter.ChannelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.CommandFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.Frontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes;
import io.zenwave360.sdk.plugins.frontmatter.DomainFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.EntityFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.EventFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.QueryFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.ServiceFrontmatter;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Generates EventCatalog {@code index.mdx} pages for domains, subdomains, and services.
 *
 * <p>Reads the typed manifest and EventCatalog enrichment model produced by
 * {@link EventCatalogArchitectureLoader}.
 */
public class EventCatalogGenerator extends Generator {

    private static final String DEFAULT_DOCS_TEMPLATE =
            "io/zenwave360/sdk/plugins/EventCatalogGenerator/docs.md";
    private static final String TEMPLATES_ROOT =
            "io/zenwave360/sdk/plugins/EventCatalogGenerator";
    private static final String DOMAIN_TEMPLATE = TEMPLATES_ROOT + "/domain.mdx";
    private static final String SUBDOMAIN_TEMPLATE = TEMPLATES_ROOT + "/subdomain.mdx";
    private static final String SERVICE_TEMPLATE = TEMPLATES_ROOT + "/service.mdx";
    private static final String CHANNEL_TEMPLATE = TEMPLATES_ROOT + "/channel.mdx";
    private static final String EVENT_TEMPLATE = TEMPLATES_ROOT + "/event.mdx";
    private static final String COMMAND_TEMPLATE = TEMPLATES_ROOT + "/command.mdx";
    private static final String QUERY_TEMPLATE = TEMPLATES_ROOT + "/query.mdx";
    private static final String ENTITY_TEMPLATE = TEMPLATES_ROOT + "/entity.mdx";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Custom Handlebars template for docs body rendering.")
    public String docsTemplate;
    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;
    @DocumentedOption(description = "Preferred source for generated frontmatter links.")
    public String linkSource;

    @Override
    @SuppressWarnings("unchecked")
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        if (manifest == null || eventCatalog == null) {
            return new GeneratedProjectFiles();
        }

        GeneratedProjectFiles files = new GeneratedProjectFiles();

        String configVersion = manifest.getConfig().getVersion() != null
                ? manifest.getConfig().getVersion()
                : "0.0.1";
        Map<String, Object> services = serviceViews(manifest, eventCatalog);

        // Domains and their subdomains
        for (ManifestDomain manifestDomain : manifest.getDomains()) {
            Map<String, Object> domain = domainView(eventCatalog, manifestDomain);
            String domainId = manifestDomain.getId();
            List<Map<String, Object>> domainServices = manifestDomain.getServices().stream()
                    .map(service -> serviceView(manifest, eventCatalog, service))
                    .toList();
            List<Map<String, Object>> childDomains = manifestDomain.getSubdomains().stream()
                    .map(subdomain -> subdomainView(eventCatalog, manifestDomain, subdomain))
                    .toList();

            files.singleFiles.add(mdxPage(
                    "domains/" + domainId + "/index.mdx",
                    domainFrontmatter(domainId, domain, configVersion, domainServices, childDomains),
                    domainBody(DOMAIN_TEMPLATE, domain, domainServices, childDomains, "")));

            for (ManifestSubdomain manifestSubdomain : manifestDomain.getSubdomains()) {
                Map<String, Object> subdomain = subdomainView(eventCatalog, manifestDomain, manifestSubdomain);
                String subdomainId = eventCatalog.catalogSubdomainId(manifestDomain, manifestSubdomain);
                List<Map<String, Object>> subdomainServices = manifestSubdomain.getServices().stream()
                        .map(service -> serviceView(manifest, eventCatalog, service))
                        .toList();

                files.singleFiles.add(mdxPage(
                        "domains/" + domainId + "/subdomains/" + subdomainId + "/index.mdx",
                        domainFrontmatter(subdomainId, subdomain, configVersion, subdomainServices, List.of()),
                        domainBody(SUBDOMAIN_TEMPLATE, subdomain, subdomainServices, List.of(), "")));
            }
        }

        // Services, events, and commands
        for (ManifestService manifestService : manifest.getServices()) {
            Map<String, Object> service = serviceView(manifest, eventCatalog, manifestService);
            String serviceId = eventCatalog.catalogServiceId(manifestService);
            String domainId = manifestService.getDomainId();
            String subdomainId = catalogSubdomainId(manifestService);
            String serviceBase = subdomainId != null && !subdomainId.isBlank()
                    ? "domains/" + domainId + "/subdomains/" + subdomainId + "/services/" + serviceId
                    : "domains/" + domainId + "/services/" + serviceId;

            files.singleFiles.add(mdxPage(
                    serviceBase + "/index.mdx",
                    serviceFrontmatter(serviceId, service, configVersion, serviceBase + "/index.mdx"),
                    serviceBody(service, renderDocs(manifestService, contextModel))));

            List<Map<String, Object>> channels = (List<Map<String, Object>>) service.getOrDefault("_channels", List.of());
            String channelBase = subdomainId != null && !subdomainId.isBlank()
                    ? "domains/" + domainId + "/subdomains/" + subdomainId + "/channels"
                    : "domains/" + domainId + "/channels";
            for (Map<String, Object> channel : channels) {
                String channelId = str(channel, "id", null);
                if (channelId == null) continue;
                files.singleFiles.add(mdxPage(
                        channelBase + "/" + channelId + "/index.mdx",
                        channelFrontmatter(channel, service),
                        channelBody(channel, service)));
            }

            // Event pages
            List<Map<String, Object>> events = (List<Map<String, Object>>) service.getOrDefault("_events", List.of());
            for (Map<String, Object> event : events) {
                String eventId = str(event, "id", null);
                if (eventId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/events/" + eventId + "/index.mdx",
                        eventFrontmatter(event, services),
                        messageBody("event", event)));
            }

            // Command pages
            List<Map<String, Object>> commands = (List<Map<String, Object>>) service.getOrDefault("_commands", List.of());
            for (Map<String, Object> command : commands) {
                String commandId = str(command, "id", null);
                if (commandId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/commands/" + commandId + "/index.mdx",
                        commandFrontmatter(command, services),
                        messageBody("command", command)));
            }

            // Query pages (from OpenAPI GET operations)
            List<Map<String, Object>> queries = (List<Map<String, Object>>) service.getOrDefault("_queries", List.of());
            for (Map<String, Object> query : queries) {
                String queryId = str(query, "id", null);
                if (queryId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/queries/" + queryId + "/index.mdx",
                        queryFrontmatter(query, services),
                        queryBody(query)));
            }

            // Entity pages (from ZDL domain models)
            List<Map<String, Object>> entities = (List<Map<String, Object>>) service.getOrDefault("_entities", List.of());
            for (Map<String, Object> entity : entities) {
                String entityId = str(entity, "id", null);
                if (entityId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/entities/" + entityId + "/index.mdx",
                        entityFrontmatter(entity, service, domainId, subdomainId, serviceId, configVersion),
                        entityBody(entity)));
            }
        }

        return files;
    }

    // -------------------------------------------------------------------------
    // Frontmatter
    // -------------------------------------------------------------------------

    private Frontmatter domainFrontmatter(String id, Map<String, Object> entry, String configVersion,
                                          List<Map<String, Object>> services, List<Map<String, Object>> childDomains) {
        String version = str(entry, "version", configVersion);
        return new DomainFrontmatter(
                commonFrontmatter(entry, id, str(entry, "name", id), version, str(entry, "description", str(entry, "summary", null)), null, null),
                toPointers(services, null),
                toPointers(listOfMaps(entry.get("agents")), version, null),
                toPointers(childDomains, version, null),
                toPointers(listOfMaps(entry.get("data-products")), version, null),
                collectEntityPointers(services),
                toPointers(listOfMaps(entry.get("flows")), version, null),
                collectMessagePointers(services, "_sends"),
                collectMessagePointers(services, "_receives"),
                null);
    }

    private Frontmatter serviceFrontmatter(String id, Map<String, Object> entry, String configVersion, String servicePagePath) {
        String version = str(entry, "_version", str(entry, "version", configVersion));
        return new ServiceFrontmatter(
                commonFrontmatter(entry, id, str(entry, "name", id), version, str(entry, "description", str(entry, "summary", null)), null, specifications(entry, servicePagePath)),
                messagePointers(entry.get("_sends")),
                messagePointers(entry.get("_receives")),
                toPointers(listOfMaps(entry.get("_entities")), version, null),
                toPointers(listOfMaps(entry.get("writesTo")), version, null),
                toPointers(listOfMaps(entry.get("readsFrom")), version, null),
                toPointers(listOfMaps(entry.get("flows")), version, null),
                bool(entry.get("externalSystem")),
                null);
    }

    private Frontmatter eventFrontmatter(Map<String, Object> event, Map<String, Object> services) {
        return new EventFrontmatter(
                commonFrontmatter(event, str(event, "id", null), str(event, "name", str(event, "id", null)), str(event, "version", "0.0.1"),
                        str(event, "summary", null), str(event, "schemaPath", null), null),
                operation(event),
                relatedCollectionRefs(services, eventIdPredicate(str(event, "id", null), "_sends"), "services"),
                relatedCollectionRefs(services, eventIdPredicate(str(event, "id", null), "_receives"), "services"),
                channelPointers(event),
                null,
                null);
    }

    private Frontmatter commandFrontmatter(Map<String, Object> command, Map<String, Object> services) {
        return new CommandFrontmatter(
                commonFrontmatter(command, str(command, "id", null), str(command, "name", str(command, "id", null)), str(command, "version", "0.0.1"),
                        str(command, "summary", null), str(command, "schemaPath", null), null),
                operation(command),
                relatedCollectionRefs(services, eventIdPredicate(str(command, "id", null), "_sends"), "services"),
                relatedCollectionRefs(services, eventIdPredicate(str(command, "id", null), "_receives"), "services"),
                channelPointers(command),
                null,
                null);
    }

    private Frontmatter queryFrontmatter(Map<String, Object> query, Map<String, Object> services) {
        return new QueryFrontmatter(
                commonFrontmatter(query, str(query, "id", null), str(query, "name", str(query, "id", null)), str(query, "version", "0.0.1"),
                        str(query, "summary", null), str(query, "schemaPath", null), null),
                operation(query),
                relatedCollectionRefs(services, eventIdPredicate(str(query, "id", null), "_sends"), "services"),
                relatedCollectionRefs(services, eventIdPredicate(str(query, "id", null), "_receives"), "services"),
                channelPointers(query),
                null,
                null);
    }

    private Frontmatter entityFrontmatter(Map<String, Object> entity, Map<String, Object> service,
                                          String domainId, String subdomainId, String serviceId, String configVersion) {
        String serviceVersion = str(service, "_version", str(service, "version", configVersion));
        String effectiveDomainId = subdomainId != null ? subdomainId : domainId;
        String entityVersion = str(entity, "version", configVersion);
        return new EntityFrontmatter(
                commonFrontmatter(entity, str(entity, "id", null), str(entity, "name", str(entity, "id", null)), entityVersion,
                        str(entity, "summary", null), null, null),
                bool(entity.get("aggregateRoot")),
                str(entity, "identifier", null),
                entityProperties(entity),
                List.of(collectionRef(serviceId, serviceVersion)),
                effectiveDomainId != null ? List.of(collectionRef(effectiveDomainId, configVersion)) : null,
                null);
    }

    private Frontmatter channelFrontmatter(Map<String, Object> channel, Map<String, Object> service) {
        return new ChannelFrontmatter(
                commonFrontmatter(channel, str(channel, "id", null), str(channel, "name", str(channel, "id", null)), str(channel, "version", "0.0.1"),
                        str(channel, "summary", null), null, null),
                null,
                str(channel, "address", null),
                strings(channel.get("protocols")),
                str(channel, "deliveryGuarantee", null),
                null,
                null,
                channelMessages(channel, service),
                null);
    }

    // -------------------------------------------------------------------------
    // Docs rendering
    // -------------------------------------------------------------------------

    private String renderDocs(ManifestService manifestService, Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        BlockingZenWaveManifestLoader manifestRuntime =
                (BlockingZenWaveManifestLoader) contextModel.get("manifestRuntime");
        if (manifest != null && manifestRuntime != null && manifestService != null && !manifestService.getDocs().isEmpty()) {
            try {
                ManifestLoadOptions contentOptions = new ManifestLoadOptions()
                        .withPreferredSource(preferredSource)
                        .withFallback(allowFallback == null || allowFallback);
                Map<String, String> resolvedDocs =
                        manifestRuntime.loadAvailableServiceDocs(manifest, manifestService, contentOptions);
                return renderDocsTemplate(resolvedDocs);
            } catch (Exception e) {
                log.warn("Cannot load docs for {}: {}", manifestService.getServiceRef(), e.getMessage());
            }
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Page body rendering
    // -------------------------------------------------------------------------

    private String domainBody(String template, Map<String, Object> entry, List<Map<String, Object>> services,
                              List<Map<String, Object>> childDomains, String docsBody) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entry", entry);
        model.put("services", services);
        model.put("childDomains", childDomains);
        model.put("summary", str(entry, "summary", str(entry, "description", null)));
        model.put("hasEntities", hasEntities(services));
        model.put("hasRelatedResources", !childDomains.isEmpty() || !services.isEmpty());
        model.put("docsBody", docsBody);
        return renderBodyTemplate(template, model);
    }

    private String serviceBody(Map<String, Object> service, String docsBody) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("service", service);
        model.put("summary", str(service, "summary", str(service, "description", null)));
        model.put("hasMessages", hasMessages(service));
        model.put("hasEntities", !listOfMaps(service.get("_entities")).isEmpty());
        model.put("hasSpecifications", hasSpecifications(service));
        model.put("docsBody", docsBody);
        return renderBodyTemplate(SERVICE_TEMPLATE, model);
    }

    private String messageBody(String resourceType, Map<String, Object> resource) {
        String remoteSchemaUrl = str(resource, "_remoteSchemaUrl", null);
        String remoteSchemaComponentMessage = str(resource, "_remoteSchemaComponentMessage", null);
        String remoteSchemaChannel = str(resource, "_remoteSchemaChannel", null);
        String remoteSchemaChannelMessage = str(resource, "_remoteSchemaChannelMessage", null);
        boolean hasRemoteSchema = remoteSchemaUrl != null && !remoteSchemaUrl.isBlank()
                && ((remoteSchemaComponentMessage != null && !remoteSchemaComponentMessage.isBlank())
                || (remoteSchemaChannel != null && !remoteSchemaChannel.isBlank()
                && remoteSchemaChannelMessage != null && !remoteSchemaChannelMessage.isBlank()));
        String summary = str(resource, "summary", null);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("resource", resource);
        model.put("resourceType", resourceType);
        model.put("summary", summary != null && !summary.isBlank()
                ? summary
                : "Generated " + resourceType + " reference page.");
        model.put("hasRemoteSchema", hasRemoteSchema);
        model.put("remoteSchemaUrl", hasRemoteSchema ? escapeAttribute(remoteSchemaUrl) : null);
        model.put("remoteSchemaComponentMessage", hasRemoteSchema ? escapeNullableAttribute(remoteSchemaComponentMessage) : null);
        model.put("remoteSchemaChannel", hasRemoteSchema ? escapeNullableAttribute(remoteSchemaChannel) : null);
        model.put("remoteSchemaChannelMessage", hasRemoteSchema ? escapeNullableAttribute(remoteSchemaChannelMessage) : null);
        model.put("schemaPath", escapeNullableAttribute(str(resource, "schemaPath", null)));
        return renderBodyTemplate("command".equals(resourceType) ? COMMAND_TEMPLATE : EVENT_TEMPLATE, model);
    }

    private String queryBody(Map<String, Object> query) {
        String remoteSchemaUrl = str(query, "_remoteSchemaUrl", null);
        String operationId = str(query, "_remoteSchemaOperationId", null);
        boolean hasRemoteSchema = remoteSchemaUrl != null && !remoteSchemaUrl.isBlank()
                && operationId != null && !operationId.isBlank();
        String summary = str(query, "summary", null);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("query", query);
        model.put("summary", summary != null && !summary.isBlank()
                ? summary
                : "Generated query reference page.");
        model.put("hasRemoteSchema", hasRemoteSchema);
        model.put("remoteSchemaUrl", hasRemoteSchema ? escapeAttribute(remoteSchemaUrl) : null);
        model.put("operationId", hasRemoteSchema ? escapeAttribute(operationId) : null);
        model.put("operationTarget", escapeNullableAttribute(str(query, "_remoteSchemaOperationTarget", "response")));
        model.put("statusCode", escapeNullableAttribute(str(query, "_remoteSchemaStatusCode", null)));
        model.put("mediaType", escapeNullableAttribute(str(query, "_remoteSchemaMediaType", null)));
        model.put("schemaPath", escapeNullableAttribute(str(query, "schemaPath", null)));
        return renderBodyTemplate(QUERY_TEMPLATE, model);
    }

    private String entityBody(Map<String, Object> entity) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entity", entity);
        model.put("summary", str(entity, "summary", null));
        model.put("hasRelationships", hasRelationships(entity));
        return renderBodyTemplate(ENTITY_TEMPLATE, model);
    }

    private String channelBody(Map<String, Object> channel, Map<String, Object> service) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("channel", channel);
        model.put("service", service);
        model.put("summary", str(channel, "summary", "Generated channel reference page."));
        model.put("hasMessages", channelMessages(channel, service) != null);
        return renderBodyTemplate(CHANNEL_TEMPLATE, model);
    }

    private String renderBodyTemplate(String templatePath, Map<String, Object> bodyModel) {
        Map<String, Object> model = new LinkedHashMap<>(asConfigurationMap());
        model.putAll(bodyModel);
        TemplateOutput output = getTemplateEngine().processTemplate(
                model,
                new TemplateInput(templatePath, "body"));
        return output != null ? output.getContent() : "";
    }

    private boolean hasSpecifications(Map<String, Object> service) {
        return specifications(service, "index.mdx") != null;
    }

    private boolean hasMessages(Map<String, Object> service) {
        return !strings(service.get("_sends")).isEmpty() || !strings(service.get("_receives")).isEmpty();
    }

    private boolean hasEntities(List<Map<String, Object>> services) {
        return services.stream().anyMatch(service -> !listOfMaps(service.get("_entities")).isEmpty());
    }

    private boolean hasRelationships(Map<String, Object> entity) {
        return listOfMaps(entity.get("properties")).stream()
                .anyMatch(property -> str(property, "references", null) != null);
    }

    private String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;");
    }

    private String escapeNullableAttribute(String value) {
        return value != null && !value.isBlank() ? escapeAttribute(value) : null;
    }

    // -------------------------------------------------------------------------
    // MDX page assembly
    // -------------------------------------------------------------------------

    private TemplateOutput mdxPage(String targetFile, Frontmatter frontmatter, String body) {
        return new TemplateOutput(targetFile, buildMdxContent(frontmatter.toMap(), body));
    }

    private String buildMdxContent(Map<String, Object> frontmatter, String body) {
        String frontmatterYaml = serializeToYaml(frontmatter);
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append(frontmatterYaml);
        sb.append("---\n");
        if (body != null && !body.isBlank()) {
            sb.append(body);
        }
        return sb.toString();
    }

    private String serializeToYaml(Map<String, Object> map) {
        try {
            // Jackson YAML prepends '---\n'; strip it so we control the delimiters.
            String yaml = yamlMapper.writeValueAsString(map);
            return yaml.startsWith("---\n") ? yaml.substring(4) : yaml;
        } catch (Exception e) {
            throw new RuntimeException("Cannot serialize frontmatter to YAML", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> serviceViews(ZenWaveManifest manifest, EventCatalogModel eventCatalog) {
        Map<String, Object> services = new LinkedHashMap<>();
        for (ManifestService service : manifest.getServices()) {
            services.put(eventCatalog.catalogServiceId(service), serviceView(manifest, eventCatalog, service));
        }
        return services;
    }

    private Map<String, Object> serviceView(ZenWaveManifest manifest, EventCatalogModel eventCatalog,
                                            ManifestService service) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", eventCatalog.catalogServiceId(service));
        view.put("serviceRef", service.getServiceRef());
        putIfNotNull(view, "version", service.documentVersion());
        putIfNotNull(view, "name", service.getName());
        putIfNotNull(view, "description", service.getDescription());
        view.put("domain", service.getDomainId());
        putIfNotNull(view, "subdomain", catalogSubdomainId(service));
        if (!service.getDocs().isEmpty()) {
            view.put("docs", new LinkedHashMap<>(service.getDocs()));
        }
        if (!service.getArtifacts().isEmpty()) {
            List<Map<String, Object>> artifacts = new ArrayList<>();
            for (ManifestArtifact artifact : service.getArtifacts()) {
                Map<String, Object> artifactView = new LinkedHashMap<>();
                putIfNotNull(artifactView, "name", artifact.getName());
                putIfNotNull(artifactView, "artifactId", artifact.getArtifactId());
                artifactView.put("type", artifact.getType());
                artifactView.put("path", artifact.getPath());
                putIfNotNull(artifactView, "version", artifact.getVersion());
                artifactView.putAll(eventCatalog.artifactData(service, artifact));
                artifacts.add(artifactView);
            }
            view.put("artifacts", artifacts);
        }
        if (!service.getConsumers().isEmpty()) {
            view.put("consumers", service.getConsumers().stream()
                    .map(reference -> {
                        ManifestService consumer = manifest.findService(reference);
                        return consumer != null
                                ? eventCatalog.catalogServiceId(consumer)
                                : reference.replace('/', '.');
                    })
                    .toList());
        }
        view.putAll(eventCatalog.serviceData(service));
        return view;
    }

    private Map<String, Object> domainView(EventCatalogModel eventCatalog, ManifestDomain domain) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", domain.getId());
        putIfNotNull(view, "version", domain.getVersion());
        putIfNotNull(view, "name", domain.getName());
        putIfNotNull(view, "description", domain.getDescription());
        view.putAll(eventCatalog.domainData(domain));
        return view;
    }

    private Map<String, Object> subdomainView(EventCatalogModel eventCatalog, ManifestDomain domain,
                                              ManifestSubdomain subdomain) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", eventCatalog.catalogSubdomainId(domain, subdomain));
        putIfNotNull(view, "version", subdomain.getVersion());
        putIfNotNull(view, "name", subdomain.getName());
        putIfNotNull(view, "description", subdomain.getDescription());
        view.putAll(eventCatalog.subdomainData(domain, subdomain));
        return view;
    }

    private String catalogSubdomainId(ManifestService service) {
        if (service.getSubdomainKey() == null) {
            return null;
        }
        return service.getSubdomainId().equals(service.getSubdomainKey())
                ? service.getDomainId() + "." + service.getSubdomainId()
                : service.getSubdomainId();
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private String str(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private FrontmatterTypes.CommonFrontmatter commonFrontmatter(Map<String, Object> entry, String id, String name, String version,
                                                                 String summary, String schemaPath,
                                                                 List<FrontmatterTypes.SpecificationFrontmatter> specifications) {
        return new FrontmatterTypes.CommonFrontmatter(
                id,
                name,
                summary,
                version,
                draft(entry.get("draft")),
                badges(entry.get("badges")),
                owners(entry.get("owners")),
                schemaPath,
                specifications,
                null,
                repository(entry),
                bool(entry.get("hidden")),
                str(entry, "editUrl", null),
                null,
                null,
                deprecated(entry.get("deprecated")),
                bool(entry.get("visualiser")),
                attachments(entry.get("attachments")),
                toResourcePointers(listOfMaps(entry.get("diagrams")), "diagram"),
                strings(entry.get("versions")),
                str(entry, "latestVersion", null));
    }

    private FrontmatterTypes.RepositoryFrontmatter repository(Map<String, Object> entry) {
        String repositoryUrl = str(entry, "repository", null);
        if (repositoryUrl == null && entry.get("repository") instanceof Map<?, ?> repository) {
            repositoryUrl = str((Map<String, Object>) repository, "url", null);
        }
        if (repositoryUrl == null) {
            return null;
        }
        String language = entry.get("repository") instanceof Map<?, ?> repository
                ? str((Map<String, Object>) repository, "language", null)
                : null;
        return new FrontmatterTypes.RepositoryFrontmatter(language, repositoryUrl);
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.SpecificationFrontmatter> specifications(Map<String, Object> entry, String servicePagePath) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) entry.getOrDefault("artifacts", List.of());
        if (specs.isEmpty()) {
            return null;
        }

        List<FrontmatterTypes.SpecificationFrontmatter> result = new ArrayList<>();
        for (Map<String, Object> spec : specs) {
            String type = str(spec, "type", null);
            if (!List.of("asyncapi", "openapi", "graphql").contains(type)) {
                continue;
            }
            String path = specPathForEventCatalog(spec, servicePagePath);
            if (path == null) {
                continue;
            }
            result.add(new FrontmatterTypes.SpecificationFrontmatter(
                    type,
                    path,
                    str(spec, "name", null),
                    mapOfStrings(spec.get("headers"))));
        }
        return result.isEmpty() ? null : result;
    }

    private String specPathForEventCatalog(Map<String, Object> spec, String servicePagePath) {
        String buildPath = str(spec, "buildPath", null);
        if (buildPath != null && !buildPath.isBlank() && new File(buildPath).isAbsolute()) {
            try {
                Path eventCatalogRoot = Paths.get(System.getProperty("user.dir"), "event-catalog-project").toAbsolutePath().normalize();
                Path serviceDir = eventCatalogRoot.resolve(servicePagePath).getParent().normalize();
                Path sourceFile = Paths.get(buildPath).toAbsolutePath().normalize();
                return serviceDir.relativize(sourceFile).toString().replace('\\', '/');
            } catch (Exception ignored) {
            }
        }
        return str(spec, "buildPath", str(spec, "linkUri", str(spec, "path", null)));
    }

    private String renderDocsTemplate(Map<String, String> resolvedDocs) {
        if (resolvedDocs == null || resolvedDocs.isEmpty()) {
            return "";
        }
        String templatePath = docsTemplate != null ? docsTemplate : DEFAULT_DOCS_TEMPLATE;
        TemplateInput templateInput = new TemplateInput(templatePath, "docs");
        Map<String, Object> model = new LinkedHashMap<>(asConfigurationMap());
        model.put("docs", resolvedDocs);
        TemplateOutput output = getTemplateEngine().processTemplate(model, templateInput);
        return output != null ? output.getContent() : "";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterServices(Map<String, Object> services, Predicate<Map<String, Object>> filter) {
        return services.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(value -> (Map<String, Object>) value)
                .filter(filter)
                .toList();
    }

    private List<FrontmatterTypes.MessagePointerFrontmatter> collectMessagePointers(List<Map<String, Object>> services, String key) {
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> service : services) {
            ids.addAll(strings(service.get(key)));
        }
        return ids.isEmpty() ? null : ids.stream()
                .map(id -> new FrontmatterTypes.MessagePointerFrontmatter(id, null, null, null, null, null))
                .toList();
    }

    private List<FrontmatterTypes.ResourcePointerFrontmatter> collectEntityPointers(List<Map<String, Object>> services) {
        List<Map<String, Object>> entities = new ArrayList<>();
        for (Map<String, Object> service : services) {
            entities.addAll(listOfMaps(service.get("_entities")));
        }
        return toPointers(entities, null);
    }

    private List<FrontmatterTypes.MessagePointerFrontmatter> messagePointers(Object value) {
        List<String> ids = strings(value);
        return ids.isEmpty() ? null : ids.stream()
                .map(id -> new FrontmatterTypes.MessagePointerFrontmatter(id, null, null, null, null, null))
                .toList();
    }

    private List<String> relatedCollectionRefs(Map<String, Object> services, Predicate<Map<String, Object>> filter, String collection) {
        return toCollectionRefs(filterServices(services, filter));
    }

    private Predicate<Map<String, Object>> eventIdPredicate(String id, String key) {
        return service -> strings(service.get(key)).contains(id);
    }

    private List<String> toCollectionRefs(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            String id = str(item, "id", null);
            String version = str(item, "_version", str(item, "version", null));
            if (id == null || version == null) {
                continue;
            }
            String ref = collectionRef(id, version);
            if (seen.add(ref)) {
                result.add(ref);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private List<String> toCollectionRefs(List<Map<String, Object>> items, String defaultVersion) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            String id = str(item, "id", null);
            String version = str(item, "_version", str(item, "version", defaultVersion));
            if (id == null || version == null) {
                continue;
            }
            String ref = collectionRef(id, version);
            if (seen.add(ref)) {
                result.add(ref);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private List<FrontmatterTypes.ResourcePointerFrontmatter> toPointers(List<Map<String, Object>> items, String defaultVersion) {
        return toPointers(items, defaultVersion, null);
    }

    private List<FrontmatterTypes.ResourcePointerFrontmatter> toPointers(List<Map<String, Object>> items, String defaultVersion, String defaultType) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<FrontmatterTypes.ResourcePointerFrontmatter> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            String id = str(item, "id", null);
            String version = str(item, "_version", str(item, "version", defaultVersion));
            String type = str(item, "type", defaultType);
            if (id == null) {
                continue;
            }
            String key = id + "|" + version + "|" + type;
            if (seen.add(key)) {
                result.add(new FrontmatterTypes.ResourcePointerFrontmatter(id, version, type));
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String collectionRef(String id, String version) {
        return id + "-" + version;
    }

    @SuppressWarnings("unchecked")
    private FrontmatterTypes.OperationFrontmatter operation(Map<String, Object> entry) {
        Object operation = entry.get("operation");
        if (!(operation instanceof Map<?, ?> map)) {
            return null;
        }
        return new FrontmatterTypes.OperationFrontmatter(
                str((Map<String, Object>) map, "method", null),
                str((Map<String, Object>) map, "path", null),
                strings(((Map<String, Object>) map).get("statusCodes")));
    }

    private List<FrontmatterTypes.ChannelPointerFrontmatter> channelPointers(Map<String, Object> entry) {
        String channelId = str(entry, "channelId", null);
        if (channelId == null) {
            return null;
        }
        return List.of(new FrontmatterTypes.ChannelPointerFrontmatter(channelId, null, null));
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.ChannelMessageFrontmatter> channelMessages(Map<String, Object> channel, Map<String, Object> service) {
        String channelId = str(channel, "id", null);
        if (channelId == null) {
            return null;
        }
        List<FrontmatterTypes.ChannelMessageFrontmatter> messages = new ArrayList<>();
        for (Map<String, Object> event : listOfMaps(service.get("_events"))) {
            if (channelId.equals(str(event, "channelId", null))) {
                messages.add(new FrontmatterTypes.ChannelMessageFrontmatter("events", str(event, "name", null), str(event, "id", null), str(event, "version", null)));
            }
        }
        for (Map<String, Object> command : listOfMaps(service.get("_commands"))) {
            if (channelId.equals(str(command, "channelId", null))) {
                messages.add(new FrontmatterTypes.ChannelMessageFrontmatter("commands", str(command, "name", null), str(command, "id", null), str(command, "version", null)));
            }
        }
        return messages.isEmpty() ? null : messages;
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.EntityPropertyFrontmatter> entityProperties(Map<String, Object> entity) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.getOrDefault("properties", List.of());
        if (properties.isEmpty()) {
            return null;
        }
        List<FrontmatterTypes.EntityPropertyFrontmatter> result = new ArrayList<>();
        for (Map<String, Object> property : properties) {
            Map<String, Object> items = property.get("items") instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
            result.add(new FrontmatterTypes.EntityPropertyFrontmatter(
                    str(property, "name", null),
                    str(property, "type", null),
                    bool(property.get("required")),
                    str(property, "description", null),
                    str(property, "references", null),
                    str(property, "referencesIdentifier", null),
                    str(property, "relationType", null),
                    strings(property.get("enum")),
                    items != null ? new FrontmatterTypes.EntityPropertyItemsFrontmatter(str(items, "type", null)) : null));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.ResourcePointerFrontmatter> toResourcePointers(List<Map<String, Object>> items, String type) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<FrontmatterTypes.ResourcePointerFrontmatter> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            String id = str(item, "id", null);
            if (id == null || !seen.add(id)) {
                continue;
            }
            result.add(new FrontmatterTypes.ResourcePointerFrontmatter(id, str(item, "version", null), type));
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            } else if (item instanceof String id) {
                result.add(Map.of("id", id));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof String string) {
                    result.add(string);
                } else if (item instanceof Map<?, ?> map) {
                    String id = str((Map<String, Object>) map, "id", null);
                    if (id != null) {
                        result.add(id);
                    }
                }
            }
            return result;
        }
        return List.of(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.BadgeFrontmatter> badges(Object value) {
        List<Map<String, Object>> badges = listOfMaps(value);
        if (badges.isEmpty()) {
            return null;
        }
        return badges.stream()
                .map(badge -> new FrontmatterTypes.BadgeFrontmatter(
                        str(badge, "content", null),
                        str(badge, "backgroundColor", null),
                        str(badge, "textColor", null),
                        str(badge, "icon", null),
                        str(badge, "url", null)))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.OwnerFrontmatter> owners(Object value) {
        if (!(value instanceof Collection<?> owners)) {
            return null;
        }
        List<FrontmatterTypes.OwnerFrontmatter> result = new ArrayList<>();
        for (Object owner : owners) {
            if (owner instanceof String id) {
                result.add(new FrontmatterTypes.OwnerFrontmatter(id));
            } else if (owner instanceof Map<?, ?> map) {
                String id = str((Map<String, Object>) map, "id", null);
                if (id != null) {
                    result.add(new FrontmatterTypes.OwnerFrontmatter(id));
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private FrontmatterTypes.DraftFrontmatter draft(Object value) {
        if (value instanceof Boolean draft && draft) {
            return new FrontmatterTypes.DraftFrontmatter(null, null);
        }
        if (value instanceof Map<?, ?> map) {
            return new FrontmatterTypes.DraftFrontmatter(
                    str((Map<String, Object>) map, "title", null),
                    str((Map<String, Object>) map, "message", null));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private FrontmatterTypes.DeprecatedFrontmatter deprecated(Object value) {
        if (value instanceof Boolean deprecated && deprecated) {
            return new FrontmatterTypes.DeprecatedFrontmatter(null, null);
        }
        if (value instanceof Map<?, ?> map) {
            return new FrontmatterTypes.DeprecatedFrontmatter(
                    str((Map<String, Object>) map, "message", null),
                    str((Map<String, Object>) map, "date", null));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<FrontmatterTypes.AttachmentFrontmatter> attachments(Object value) {
        if (!(value instanceof Collection<?> attachments)) {
            return null;
        }
        List<FrontmatterTypes.AttachmentFrontmatter> result = new ArrayList<>();
        for (Object attachment : attachments) {
            if (attachment instanceof String url) {
                result.add(new FrontmatterTypes.AttachmentFrontmatter(url, null, null, null, null));
            } else if (attachment instanceof Map<?, ?> map) {
                result.add(new FrontmatterTypes.AttachmentFrontmatter(
                        str((Map<String, Object>) map, "url", null),
                        str((Map<String, Object>) map, "title", null),
                        str((Map<String, Object>) map, "type", null),
                        str((Map<String, Object>) map, "description", null),
                        str((Map<String, Object>) map, "icon", null)));
            }
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> mapOfStrings(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return result.isEmpty() ? null : result;
    }

    private Boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : null;
    }
}
