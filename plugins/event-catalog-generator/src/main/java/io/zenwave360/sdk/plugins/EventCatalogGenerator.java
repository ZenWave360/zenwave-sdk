package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import java.io.IOException;
import java.nio.file.Files;
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
 * <p>Phase 1 scope: structural skeleton only (no events, commands, queries, or entities).
 * Reads the {@code "architecture"} map produced by {@link EventCatalogArchitectureLoader}.
 */
public class EventCatalogGenerator extends Generator {

    private static final String DEFAULT_DOCS_TEMPLATE =
            "io/zenwave360/sdk/plugins/EventCatalogGenerator/docs.md";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Custom Handlebars template for docs body rendering.")
    public String docsTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        GeneratedProjectFiles files = new GeneratedProjectFiles();

        String configVersion = configVersion(architecture);
        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());

        // Domains and their subdomains
        Map<String, Object> domains = (Map<String, Object>) architecture.getOrDefault("domains", Map.of());
        for (Map.Entry<String, Object> domainEntry : domains.entrySet()) {
            Map<String, Object> domain = (Map<String, Object>) domainEntry.getValue();
            String domainId = str(domain, "id", domainEntry.getKey());

            files.singleFiles.add(mdxPage(
                    "domains/" + domainId + "/index.mdx",
                    domainFrontmatter(domainId, domain, configVersion, domainServices(services, domainId, domains), childDomains(domain)),
                    renderDocs(domain, contextModel)));

            Map<String, Object> subdomains = (Map<String, Object>) domain.getOrDefault("subdomains", Map.of());
            for (Map.Entry<String, Object> subEntry : subdomains.entrySet()) {
                Map<String, Object> subdomain = (Map<String, Object>) subEntry.getValue();
                String subdomainId = str(subdomain, "id", subEntry.getKey());

                files.singleFiles.add(mdxPage(
                        "domains/" + domainId + "/" + subdomainId + "/index.mdx",
                        domainFrontmatter(subdomainId, subdomain, configVersion, subdomainServices(services, domains, domainId, subdomainId), List.of()),
                        renderDocs(subdomain, contextModel)));
            }
        }

        // Services, events, and commands
        for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) serviceEntry.getValue();
            String serviceId = str(service, "id", serviceEntry.getKey());
            String domainId = str(service, "domain", null);
            String subdomainKey = str(service, "subdomain", null);

            String subdomainId = resolveSubdomainId(domains, domainId, subdomainKey);
            String serviceBase = subdomainId != null && !subdomainId.isBlank()
                    ? "domains/" + domainId + "/" + subdomainId + "/services/" + serviceId
                    : "domains/" + domainId + "/services/" + serviceId;

            files.singleFiles.add(mdxPage(
                    serviceBase + "/index.mdx",
                    serviceFrontmatter(serviceId, service, configVersion),
                    renderDocs(service, contextModel)));

            List<Map<String, Object>> channels = (List<Map<String, Object>>) service.getOrDefault("_channels", List.of());
            String channelBase = subdomainId != null && !subdomainId.isBlank()
                    ? "domains/" + domainId + "/" + subdomainId + "/channels"
                    : "domains/" + domainId + "/channels";
            for (Map<String, Object> channel : channels) {
                String channelId = str(channel, "id", null);
                if (channelId == null) continue;
                files.singleFiles.add(mdxPage(
                        channelBase + "/" + channelId + "/index.mdx",
                        channelFrontmatter(channel, service),
                        ""));
            }

            // Event pages
            List<Map<String, Object>> events = (List<Map<String, Object>>) service.getOrDefault("_events", List.of());
            for (Map<String, Object> event : events) {
                String eventId = str(event, "id", null);
                if (eventId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/events/" + eventId + "/index.mdx",
                        eventFrontmatter(event, services),
                        ""));
            }

            // Command pages
            List<Map<String, Object>> commands = (List<Map<String, Object>>) service.getOrDefault("_commands", List.of());
            for (Map<String, Object> command : commands) {
                String commandId = str(command, "id", null);
                if (commandId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/commands/" + commandId + "/index.mdx",
                        commandFrontmatter(command, services),
                        ""));
            }

            // Query pages (from OpenAPI GET operations)
            List<Map<String, Object>> queries = (List<Map<String, Object>>) service.getOrDefault("_queries", List.of());
            for (Map<String, Object> query : queries) {
                String queryId = str(query, "id", null);
                if (queryId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/queries/" + queryId + "/index.mdx",
                        queryFrontmatter(query, services),
                        ""));
            }

            // Entity pages (from ZDL domain models)
            List<Map<String, Object>> entities = (List<Map<String, Object>>) service.getOrDefault("_entities", List.of());
            for (Map<String, Object> entity : entities) {
                String entityId = str(entity, "id", null);
                if (entityId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/entities/" + entityId + "/index.mdx",
                        entityFrontmatter(entity, service, domainId, subdomainId, serviceId, configVersion),
                        ""));
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
                toResourcePointers(services, "service"),
                toResourcePointers(listOfMaps(entry.get("agents")), "agent"),
                toResourcePointers(childDomains, "domain"),
                toResourcePointers(listOfMaps(entry.get("data-products")), "data-product"),
                collectEntityPointers(services),
                toResourcePointers(listOfMaps(entry.get("flows")), "flow"),
                collectMessagePointers(services, "_sends"),
                collectMessagePointers(services, "_receives"),
                null);
    }

    private Frontmatter serviceFrontmatter(String id, Map<String, Object> entry, String configVersion) {
        String version = str(entry, "_version", str(entry, "version", configVersion));
        return new ServiceFrontmatter(
                commonFrontmatter(entry, id, str(entry, "name", id), version, str(entry, "description", str(entry, "summary", null)), null, specifications(entry)),
                messagePointers(entry.get("_sends")),
                messagePointers(entry.get("_receives")),
                toResourcePointers(listOfMaps(entry.get("_entities")), "entity"),
                toResourcePointers(listOfMaps(entry.get("writesTo")), "container"),
                toResourcePointers(listOfMaps(entry.get("readsFrom")), "container"),
                toResourcePointers(listOfMaps(entry.get("flows")), "flow"),
                bool(entry.get("externalSystem")),
                null);
    }

    private Frontmatter eventFrontmatter(Map<String, Object> event, Map<String, Object> services) {
        return new EventFrontmatter(
                commonFrontmatter(event, str(event, "id", null), str(event, "name", str(event, "id", null)), str(event, "version", "0.0.1"),
                        str(event, "summary", null), str(event, "schemaPath", null), null),
                operation(event),
                relatedServices(services, eventIdPredicate(str(event, "id", null), "_sends")),
                relatedServices(services, eventIdPredicate(str(event, "id", null), "_receives")),
                channelPointers(event),
                null,
                null);
    }

    private Frontmatter commandFrontmatter(Map<String, Object> command, Map<String, Object> services) {
        return new CommandFrontmatter(
                commonFrontmatter(command, str(command, "id", null), str(command, "name", str(command, "id", null)), str(command, "version", "0.0.1"),
                        str(command, "summary", null), str(command, "schemaPath", null), null),
                operation(command),
                relatedServices(services, eventIdPredicate(str(command, "id", null), "_sends")),
                relatedServices(services, eventIdPredicate(str(command, "id", null), "_receives")),
                channelPointers(command),
                null,
                null);
    }

    private Frontmatter queryFrontmatter(Map<String, Object> query, Map<String, Object> services) {
        return new QueryFrontmatter(
                commonFrontmatter(query, str(query, "id", null), str(query, "name", str(query, "id", null)), str(query, "version", "0.0.1"),
                        str(query, "summary", null), str(query, "schemaPath", null), null),
                operation(query),
                relatedServices(services, eventIdPredicate(str(query, "id", null), "_sends")),
                relatedServices(services, eventIdPredicate(str(query, "id", null), "_receives")),
                channelPointers(query),
                null,
                null);
    }

    private Frontmatter entityFrontmatter(Map<String, Object> entity, Map<String, Object> service,
                                          String domainId, String subdomainId, String serviceId, String configVersion) {
        return new EntityFrontmatter(
                commonFrontmatter(entity, str(entity, "id", null), str(entity, "name", str(entity, "id", null)), str(entity, "version", configVersion),
                        str(entity, "summary", null), null, null),
                bool(entity.get("aggregateRoot")),
                str(entity, "identifier", null),
                entityProperties(entity),
                List.of(new FrontmatterTypes.ResourcePointerFrontmatter(serviceId, str(service, "_version", str(service, "version", configVersion)), "service")),
                List.of(new FrontmatterTypes.ResourcePointerFrontmatter(subdomainId != null ? subdomainId : domainId, null, "domain")),
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

    @SuppressWarnings("unchecked")
    private String renderDocs(Map<String, Object> entry, Map<String, Object> contextModel) {
        Object docsObj = entry.get("docs");
        if (!(docsObj instanceof Map<?,?> docsMap)) {
            return "";
        }

        String repository = str(entry, "repository", ".");
        Map<String, String> resolvedDocs = new LinkedHashMap<>();
        for (Map.Entry<?, ?> docEntry : docsMap.entrySet()) {
            String key = docEntry.getKey().toString();
            String fileName = docEntry.getValue().toString();
            File docFile = new File(fileName);
            if (!docFile.isAbsolute()) {
                docFile = new File(repository, fileName);
            }
            if (docFile.exists()) {
                try {
                    resolvedDocs.put(key, Files.readString(docFile.toPath()));
                } catch (IOException e) {
                    log.warn("Cannot read doc file {}: {}", docFile.getAbsolutePath(), e.getMessage());
                }
            } else {
                log.warn("Doc file not found: {}", docFile.getAbsolutePath());
            }
        }

        if (resolvedDocs.isEmpty()) {
            return "";
        }

        String templatePath = docsTemplate != null ? docsTemplate : DEFAULT_DOCS_TEMPLATE;
        TemplateInput templateInput = new TemplateInput(templatePath, "docs");
        Map<String, Object> model = new LinkedHashMap<>(asConfigurationMap());
        model.put("docs", resolvedDocs);
        TemplateOutput output = getTemplateEngine().processTemplate(model, templateInput);
        return output != null ? output.getContent() : "";
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

    private String configVersion(Map<String, Object> architecture) {
        Object config = architecture.get("config");
        if (config instanceof Map<?,?> configMap) {
            Object version = configMap.get("version");
            if (version != null) return version.toString();
        }
        return "0.0.1";
    }

    @SuppressWarnings("unchecked")
    private String resolveSubdomainId(Map<String, Object> domains, String domainId, String subdomainKey) {
        if (domainId == null || subdomainKey == null) return subdomainKey;
        Object domainObj = domains.get(domainId);
        if (!(domainObj instanceof Map<?, ?>)) {
            domainObj = domains.values().stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .filter(domain -> domainId.equals(str(domain, "id", null)))
                    .findFirst()
                    .orElse(null);
        }
        if (!(domainObj instanceof Map<?,?> domain)) return subdomainKey;
        Object subdomainsObj = ((Map<String, Object>) domain).get("subdomains");
        if (!(subdomainsObj instanceof Map<?,?> subdomains)) return subdomainKey;
        Object subdomainObj = ((Map<String, Object>) subdomains).get(subdomainKey);
        if (!(subdomainObj instanceof Map<?,?> subdomain)) return subdomainKey;
        return str((Map<String, Object>) subdomain, "id", subdomainKey);
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
    private List<FrontmatterTypes.SpecificationFrontmatter> specifications(Map<String, Object> entry) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) entry.getOrDefault("specs", List.of());
        if (specs.isEmpty()) {
            return null;
        }

        List<FrontmatterTypes.SpecificationFrontmatter> result = new ArrayList<>();
        for (Map<String, Object> spec : specs) {
            String type = str(spec, "type", null);
            if (!List.of("asyncapi", "openapi", "graphql").contains(type)) {
                continue;
            }
            String path = str(spec, "resolvedPath", str(spec, "path", null));
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

    private List<Map<String, Object>> domainServices(Map<String, Object> services, String domainId, Map<String, Object> domains) {
        return filterServices(services, service -> domainId.equals(str(service, "domain", null))
                && resolveSubdomainId(domains, domainId, str(service, "subdomain", null)) == null);
    }

    private List<Map<String, Object>> subdomainServices(Map<String, Object> services, Map<String, Object> domains,
                                                        String domainId, String subdomainId) {
        return filterServices(services, service -> domainId.equals(str(service, "domain", null))
                && subdomainId.equals(resolveSubdomainId(domains, domainId, str(service, "subdomain", null))));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childDomains(Map<String, Object> domain) {
        Object subdomains = domain.get("subdomains");
        if (!(subdomains instanceof Map<?, ?> map)) {
            return List.of();
        }
        return map.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(value -> (Map<String, Object>) value)
                .toList();
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
        return toResourcePointers(entities, "entity");
    }

    private List<FrontmatterTypes.MessagePointerFrontmatter> messagePointers(Object value) {
        List<String> ids = strings(value);
        return ids.isEmpty() ? null : ids.stream()
                .map(id -> new FrontmatterTypes.MessagePointerFrontmatter(id, null, null, null, null, null))
                .toList();
    }

    private List<FrontmatterTypes.ResourcePointerFrontmatter> relatedServices(Map<String, Object> services, Predicate<Map<String, Object>> filter) {
        return toResourcePointers(filterServices(services, filter), "service");
    }

    private Predicate<Map<String, Object>> eventIdPredicate(String id, String key) {
        return service -> strings(service.get(key)).contains(id);
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
