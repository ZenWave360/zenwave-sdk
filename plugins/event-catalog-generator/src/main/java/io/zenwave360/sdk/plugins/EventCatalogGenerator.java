package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.plugins.frontmatter.CommandFrontmatter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        // Domains and their subdomains
        Map<String, Object> domains = (Map<String, Object>) architecture.getOrDefault("domains", Map.of());
        for (Map.Entry<String, Object> domainEntry : domains.entrySet()) {
            Map<String, Object> domain = (Map<String, Object>) domainEntry.getValue();
            String domainId = str(domain, "id", domainEntry.getKey());

            files.singleFiles.add(mdxPage(
                    "domains/" + domainId + "/index.mdx",
                    domainFrontmatter(domainId, domain, configVersion),
                    renderDocs(domain, contextModel)));

            Map<String, Object> subdomains = (Map<String, Object>) domain.getOrDefault("subdomains", Map.of());
            for (Map.Entry<String, Object> subEntry : subdomains.entrySet()) {
                Map<String, Object> subdomain = (Map<String, Object>) subEntry.getValue();
                String subdomainId = str(subdomain, "id", subEntry.getKey());

                files.singleFiles.add(mdxPage(
                        "domains/" + domainId + "/" + subdomainId + "/index.mdx",
                        domainFrontmatter(subdomainId, subdomain, configVersion),
                        renderDocs(subdomain, contextModel)));
            }
        }

        // Services, events, and commands
        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());
        for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) serviceEntry.getValue();
            String serviceId = str(service, "id", serviceEntry.getKey());
            String domainId = str(service, "domain", null);
            String subdomainKey = str(service, "subdomain", null);

            String subdomainId = resolveSubdomainId(domains, domainId, subdomainKey);
            String serviceBase = "domains/" + domainId + "/" + subdomainId + "/services/" + serviceId;

            files.singleFiles.add(mdxPage(
                    serviceBase + "/index.mdx",
                    serviceFrontmatter(serviceId, service, configVersion),
                    renderDocs(service, contextModel)));

            // Event pages
            List<Map<String, Object>> events = (List<Map<String, Object>>) service.getOrDefault("_events", List.of());
            for (Map<String, Object> event : events) {
                String eventId = str(event, "id", null);
                if (eventId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/events/" + eventId + "/index.mdx",
                        eventFrontmatter(event),
                        ""));
            }

            // Command pages
            List<Map<String, Object>> commands = (List<Map<String, Object>>) service.getOrDefault("_commands", List.of());
            for (Map<String, Object> command : commands) {
                String commandId = str(command, "id", null);
                if (commandId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/commands/" + commandId + "/index.mdx",
                        commandFrontmatter(command),
                        ""));
            }

            // Query pages (from OpenAPI GET operations)
            List<Map<String, Object>> queries = (List<Map<String, Object>>) service.getOrDefault("_queries", List.of());
            for (Map<String, Object> query : queries) {
                String queryId = str(query, "id", null);
                if (queryId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/queries/" + queryId + "/index.mdx",
                        queryFrontmatter(query),
                        ""));
            }

            // Entity pages (from ZDL domain models)
            List<Map<String, Object>> entities = (List<Map<String, Object>>) service.getOrDefault("_entities", List.of());
            for (Map<String, Object> entity : entities) {
                String entityId = str(entity, "id", null);
                if (entityId == null) continue;
                files.singleFiles.add(mdxPage(
                        serviceBase + "/entities/" + entityId + "/index.mdx",
                        entityFrontmatter(entity),
                        ""));
            }
        }

        return files;
    }

    // -------------------------------------------------------------------------
    // Frontmatter
    // -------------------------------------------------------------------------

    private Map<String, Object> domainFrontmatter(String id, Map<String, Object> entry, String configVersion) {
        DomainFrontmatter fm = new DomainFrontmatter();
        fm.id = id;
        fm.name = str(entry, "name", id);
        fm.version = configVersion;
        return fm.toMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> serviceFrontmatter(String id, Map<String, Object> entry, String configVersion) {
        ServiceFrontmatter fm = new ServiceFrontmatter();
        fm.id = id;
        fm.name = str(entry, "name", id);
        // AsyncAPI version takes priority; fall back to explicit service version, then config version
        String asyncApiVersion = str(entry, "_version", null);
        fm.version = asyncApiVersion != null ? asyncApiVersion : str(entry, "version", configVersion);
        fm.sends = (List<String>) entry.get("_sends");
        fm.receives = (List<String>) entry.get("_receives");
        fm.specifications = (List<String>) entry.get("_specifications");
        return fm.toMap();
    }

    private Map<String, Object> eventFrontmatter(Map<String, Object> event) {
        EventFrontmatter fm = new EventFrontmatter();
        fm.id = str(event, "id", null);
        fm.name = str(event, "name", fm.id);
        fm.version = str(event, "version", "0.0.1");
        fm.schemaPath = str(event, "schemaPath", null);
        return fm.toMap();
    }

    private Map<String, Object> commandFrontmatter(Map<String, Object> command) {
        CommandFrontmatter fm = new CommandFrontmatter();
        fm.id = str(command, "id", null);
        fm.name = str(command, "name", fm.id);
        fm.version = str(command, "version", "0.0.1");
        fm.schemaPath = str(command, "schemaPath", null);
        return fm.toMap();
    }

    private Map<String, Object> queryFrontmatter(Map<String, Object> query) {
        QueryFrontmatter fm = new QueryFrontmatter();
        fm.id = str(query, "id", null);
        fm.name = str(query, "name", fm.id);
        fm.version = str(query, "version", "0.0.1");
        fm.schemaPath = str(query, "schemaPath", null);
        return fm.toMap();
    }

    private Map<String, Object> entityFrontmatter(Map<String, Object> entity) {
        EntityFrontmatter fm = new EntityFrontmatter();
        fm.id = str(entity, "id", null);
        fm.name = str(entity, "name", fm.id);
        fm.version = str(entity, "version", "0.0.1");
        fm.summary = str(entity, "summary", null);
        Object agg = entity.get("aggregateRoot");
        fm.aggregateRoot = Boolean.TRUE.equals(agg) ? true : null;
        return fm.toMap();
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
            File docFile = new File(repository, fileName);
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

    private TemplateOutput mdxPage(String targetFile, Map<String, Object> frontmatter, String body) {
        return new TemplateOutput(targetFile, buildMdxContent(frontmatter, body));
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
}
