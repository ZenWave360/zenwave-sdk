package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class EventCatalogImporterGenerator extends Generator {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    @DocumentedOption(description = "Path to the EventCatalog source tree.")
    public String inputFolder;

    @DocumentedOption(description = "Path to the generated zenwave-architecture.yml file.")
    public String outputFile;

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        if (inputFolder == null || outputFile == null) {
            throw new IllegalStateException("inputFolder and outputFile are required");
        }

        Path root = Path.of(inputFolder).toAbsolutePath().normalize();
        Path output = Path.of(outputFile).toAbsolutePath().normalize();
        Path outputRoot = output.getParent() != null ? output.getParent() : Path.of(".").toAbsolutePath();

        ImportModel model = readCatalog(root);
        GeneratedProjectFiles files = new GeneratedProjectFiles();
        Map<String, Object> manifest = buildManifest(model, files, outputRoot);
        files.singleFiles.add(new TemplateOutput(outputRoot.relativize(output).toString().replace('\\', '/'), toYaml(manifest)));
        printReport(model);
        return files;
    }

    private ImportModel readCatalog(Path root) {
        ImportModel model = new ImportModel(root);
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isMarkdown)
                    .filter(path -> !hasPathSegment(root.relativize(path), "node_modules"))
                    .sorted()
                    .map(path -> readPage(root, path))
                    .filter(Objects::nonNull)
                    .forEach(page -> model.pages.add(page));
        } catch (Exception e) {
            throw new RuntimeException("Cannot read EventCatalog folder " + root, e);
        }
        return model.index();
    }

    private boolean isMarkdown(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".mdx");
    }

    private boolean hasPathSegment(Path path, String segment) {
        for (Path part : path) {
            if (segment.equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private Page readPage(Path root, Path path) {
        try {
            String content = Files.readString(path);
            FrontmatterBlock block = frontmatter(content);
            if (block == null) {
                return null;
            }
            return new Page(root.relativize(path).toString().replace('\\', '/'), yamlMapper.readValue(block.yaml, MAP_TYPE), block.body);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read EventCatalog page " + path, e);
        }
    }

    private FrontmatterBlock frontmatter(String content) {
        if (content == null || !content.startsWith("---")) {
            return null;
        }
        int start = content.startsWith("---\r\n") ? 5 : 4;
        int end = content.indexOf("\n---", start);
        if (end < 0) {
            return null;
        }
        int bodyStart = content.indexOf('\n', end + 4);
        String body = bodyStart >= 0 ? content.substring(bodyStart + 1).trim() : "";
        return new FrontmatterBlock(content.substring(start, end).trim(), body);
    }

    private Map<String, Object> buildManifest(ImportModel model, GeneratedProjectFiles files, Path outputRoot) {
        Map<String, Object> manifest = ordered();
        manifest.put("config", config(model));
        Map<String, Object> domains = ordered();
        manifest.put("domains", domains);

        for (DomainRef domainRef : model.domains.values()) {
            Page domainPage = domainRef.page;
            Map<String, Object> domain = entry(domainPage);
            domains.put(domainRef.key(), domain);

            Map<String, Object> subdomains = ordered();
            domain.put("subdomains", subdomains);
            for (DomainRef subdomainRef : domainRef.subdomains.values()) {
                Page subdomainPage = subdomainRef.page;
                Map<String, Object> subdomain = entry(subdomainPage);
                subdomains.put(subdomainRef.key(), subdomain);

                Map<String, Object> services = ordered();
                subdomain.put("services", services);
                for (ServiceRef serviceRef : subdomainRef.services.values()) {
                    Map<String, Object> service = serviceEntry(model.root, serviceRef, files, outputRoot);
                    services.put(serviceRef.key(), service);
                }
            }
        }
        return manifest;
    }

    private Map<String, Object> config(ImportModel model) {
        Map<String, Object> config = ordered();
        config.put("title", "Imported EventCatalog");
        config.put("version", "0.0.1");
        config.put("contentResolution", List.of("workspace"));
        return config;
    }

    private Map<String, Object> serviceEntry(Path inputRoot, ServiceRef serviceRef, GeneratedProjectFiles files, Path outputRoot) {
        Page page = serviceRef.page;
        Map<String, Object> service = entry(page);
        String servicePath = servicePath(serviceRef);
        addDocs(service, files, outputRoot, servicePath + "/EVENT_CATALOG.md", "EVENT_CATALOG.md", page);

        List<Map<String, Object>> artifacts = artifacts(inputRoot, outputRoot, servicePath, serviceRef);
        if (!serviceRef.entities.isEmpty()) {
            artifacts.add(artifact("zdl", "domain-model.zdl"));
            files.singleFiles.add(new TemplateOutput(toRelativeOutput(outputRoot, servicePath + "/domain-model.zdl"), zdl(serviceRef)));
            serviceRef.report.resolved("zdl generated from entity pages");
        }
        service.put("artifacts", artifacts);
        return service;
    }

    private List<Map<String, Object>> artifacts(Path inputRoot, Path outputRoot, String servicePath, ServiceRef serviceRef) {
        List<Map<String, Object>> artifacts = new ArrayList<>();
        Set<String> types = new LinkedHashSet<>();
        for (Map<String, Object> spec : listOfMaps(serviceRef.page.frontmatter.get("specifications"))) {
            String type = str(spec.get("type"));
            String path = str(spec.get("path"));
            if (type == null || path == null) {
                continue;
            }
            artifacts.add(artifact(type, importedArtifactPath(inputRoot, outputRoot, servicePath, serviceRef.page, path)));
            types.add(type);
            serviceRef.report.resolved(type + " specification " + path);
        }

        if ((!serviceRef.events.isEmpty() || !serviceRef.commands.isEmpty()) && types.stream().noneMatch(type -> type.startsWith("asyncapi"))) {
            serviceRef.report.unresolved("asyncapi source for events/commands");
        }
        if (!serviceRef.queries.isEmpty() && !types.contains("openapi")) {
            serviceRef.report.unresolved("openapi source for queries");
        }
        addOwnedArtifacts(artifacts, serviceRef.flows, "flow");
        addOwnedArtifacts(artifacts, serviceRef.dataProducts, "data-product");
        addOwnedArtifacts(artifacts, serviceRef.diagrams, "diagram");
        return artifacts;
    }

    private void addOwnedArtifacts(List<Map<String, Object>> artifacts, List<Page> pages, String type) {
        for (Page page : pages) {
            Map<String, Object> artifact = artifact(type, page.path);
            artifacts.add(artifact);
        }
    }

    private Map<String, Object> entry(Page page) {
        Map<String, Object> entry = ordered();
        putIfPresent(entry, "version", page.frontmatter.get("version"));
        putIfPresent(entry, "name", page.frontmatter.get("name"));
        putIfPresent(entry, "description", page.frontmatter.getOrDefault("summary", page.frontmatter.get("description")));
        return entry;
    }

    private void addDocs(Map<String, Object> entry, GeneratedProjectFiles files, Path outputRoot, String docPath, String manifestPath, Page page) {
        if (page.body == null || page.body.isBlank()) {
            return;
        }
        Map<String, Object> docs = ordered();
        docs.put("content", manifestPath);
        entry.put("docs", docs);
        files.singleFiles.add(new TemplateOutput(toRelativeOutput(outputRoot, docPath), page.body.strip() + "\n"));
    }

    private String toRelativeOutput(Path outputRoot, String path) {
        return outputRoot.relativize(outputRoot.resolve(path).normalize()).toString().replace('\\', '/');
    }

    private String servicePath(ServiceRef serviceRef) {
        return String.join("/",
                serviceRef.domainId,
                lastSegment(serviceRef.subdomainId),
                lastSegment(serviceRef.id));
    }

    private String zdl(ServiceRef serviceRef) {
        StringBuilder builder = new StringBuilder();
        builder.append("config {\n");
        builder.append("    title \"").append(escape(serviceRef.name())).append("\"\n");
        builder.append("}\n\n");
        for (Page entity : serviceRef.entities) {
            String name = str(entity.frontmatter.getOrDefault("name", javaTypeName(str(entity.frontmatter.get("id")))));
            String summary = str(entity.frontmatter.get("summary"));
            if (summary != null) {
                builder.append("/**\n * ").append(escape(summary)).append("\n */\n");
            }
            if (Boolean.TRUE.equals(entity.frontmatter.get("aggregateRoot"))) {
                builder.append("@aggregate\n");
            }
            builder.append("entity ").append(javaTypeName(name)).append(" {\n");
            for (Map<String, Object> property : listOfMaps(entity.frontmatter.get("properties"))) {
                builder.append("    ").append(str(property.get("name"))).append(" ")
                        .append(zdlType(str(property.get("type"))));
                if (Boolean.TRUE.equals(property.get("required"))) {
                    builder.append(" required");
                }
                builder.append("\n");
            }
            builder.append("}\n\n");
        }
        return builder.toString();
    }

    private String toYaml(Map<String, Object> manifest) {
        try {
            return "# yaml-language-server: $schema=https://schemas.zenwave360.io/zenwave-architecture/latest/schema.json\n\n"
                    + yamlMapper.writeValueAsString(manifest);
        } catch (Exception e) {
            throw new RuntimeException("Cannot render imported manifest", e);
        }
    }

    private void printReport(ImportModel model) {
        System.out.println("EventCatalog import report");
        System.out.println("  Domains: " + model.domains.size());
        System.out.println("  Services: " + model.services.size());
        for (ServiceRef service : model.services.values()) {
            System.out.println("  Service " + service.id + ":");
            if (service.report.resolved.isEmpty() && service.report.unresolved.isEmpty()) {
                System.out.println("    clean: no derived artifacts");
            }
            service.report.resolved.forEach(line -> System.out.println("    clean: " + line));
            service.report.unresolved.forEach(line -> System.out.println("    unresolved: " + line));
        }
    }

    private Map<String, Object> artifact(String type, String path) {
        Map<String, Object> artifact = ordered();
        artifact.put("type", type);
        artifact.put("path", path);
        return artifact;
    }

    private String importedArtifactPath(Path inputRoot, Path outputRoot, String servicePath, Page page, String artifactPath) {
        if (artifactPath.startsWith("http://") || artifactPath.startsWith("https://")) {
            return artifactPath;
        }
        Path rawPath = Path.of(artifactPath);
        if (rawPath.isAbsolute()) {
            return rawPath.toString().replace('\\', '/');
        }
        Path sourcePageDirectory = inputRoot.resolve(page.path).getParent();
        Path sourceArtifact = sourcePageDirectory.resolve(rawPath).normalize();
        Path targetServiceDirectory = outputRoot.resolve(servicePath).normalize();
        return targetServiceDirectory.relativize(sourceArtifact).toString().replace('\\', '/');
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null && !value.toString().isBlank()) {
            map.put(key, value);
        }
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> (Map<String, Object>) map)
                .toList();
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }

    private String lastSegment(String id) {
        if (id == null || !id.contains(".")) {
            return id;
        }
        return id.substring(id.lastIndexOf('.') + 1);
    }

    private String javaTypeName(String value) {
        if (value == null || value.isBlank()) {
            return "ImportedEntity";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : value.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "ImportedEntity" : builder.toString();
    }

    private String zdlType(String type) {
        if (type == null || type.isBlank()) {
            return "String";
        }
        return switch (type.toLowerCase()) {
            case "string", "integer", "long", "bigdecimal", "float", "double", "boolean", "instant", "localdate", "uuid" -> type;
            default -> javaTypeName(type);
        };
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private record FrontmatterBlock(String yaml, String body) {
    }

    private record Page(String path, Map<String, Object> frontmatter, String body) {
        String id() {
            Object id = frontmatter.get("id");
            return id != null ? id.toString() : null;
        }
    }

    private static class Report {
        final List<String> resolved = new ArrayList<>();
        final List<String> unresolved = new ArrayList<>();

        void resolved(String line) {
            resolved.add(line);
        }

        void unresolved(String line) {
            unresolved.add(line);
        }
    }

    private static class DomainRef {
        final String id;
        final Page page;
        final Map<String, DomainRef> subdomains = new LinkedHashMap<>();
        final Map<String, ServiceRef> services = new LinkedHashMap<>();

        DomainRef(String id, Page page) {
            this.id = id;
            this.page = page;
        }

        String key() {
            return id != null && id.contains(".") ? id.substring(id.lastIndexOf('.') + 1) : id;
        }
    }

    private static class ServiceRef {
        String id;
        final String domainId;
        final String subdomainId;
        Page page;
        final List<Page> events = new ArrayList<>();
        final List<Page> commands = new ArrayList<>();
        final List<Page> queries = new ArrayList<>();
        final List<Page> entities = new ArrayList<>();
        final List<Page> flows = new ArrayList<>();
        final List<Page> dataProducts = new ArrayList<>();
        final List<Page> diagrams = new ArrayList<>();
        final Report report = new Report();

        ServiceRef(String id, String domainId, String subdomainId, Page page) {
            this.id = id;
            this.domainId = domainId;
            this.subdomainId = subdomainId;
            this.page = page;
        }

        String key() {
            return id != null && id.contains(".") ? id.substring(id.lastIndexOf('.') + 1) : id;
        }

        String name() {
            Object name = page.frontmatter.get("name");
            return name != null ? name.toString() : id;
        }
    }

    private static class ImportModel {
        final Path root;
        final List<Page> pages = new ArrayList<>();
        final Map<String, DomainRef> domains = new LinkedHashMap<>();
        final Map<String, ServiceRef> services = new LinkedHashMap<>();

        ImportModel(Path root) {
            this.root = root;
        }

        ImportModel index() {
            pages.stream().sorted(Comparator.comparing(page -> page.path)).forEach(this::indexPage);
            return this;
        }

        private void indexPage(Page page) {
            String[] parts = page.path.split("/");
            if (parts.length == 0 || !"domains".equals(parts[0])) {
                return;
            }
            if (parts.length == 3 && "domains".equals(parts[0]) && "index.mdx".equals(parts[2])) {
                domains.put(page.id(), new DomainRef(page.id(), page));
                return;
            }
            if (parts.length == 5 && "domains".equals(parts[0]) && "subdomains".equals(parts[2]) && "index.mdx".equals(parts[4])) {
                DomainRef domain = domains.computeIfAbsent(parts[1], id -> new DomainRef(id, stub(id)));
                domain.subdomains.put(page.id(), new DomainRef(page.id(), page));
                return;
            }
            int servicesIndex = indexOf(parts, "services");
            if (servicesIndex > 0 && parts.length > servicesIndex + 2) {
                indexServicePage(page, parts, servicesIndex);
            }
        }

        private void indexServicePage(Page page, String[] parts, int servicesIndex) {
            String domainId = parts[1];
            String subdomainId = servicesIndex >= 4 ? parts[3] : null;
            String serviceId = parts[servicesIndex + 1];
            if (parts.length == servicesIndex + 3 && "index.mdx".equals(parts[servicesIndex + 2])) {
                ServiceRef service = services.computeIfAbsent(serviceId, id -> new ServiceRef(id, domainId, subdomainId, stub(id)));
                service.id = page.id();
                service.page = page;
                if (!serviceId.equals(service.id)) {
                    services.remove(serviceId);
                }
                services.put(service.id, service);
                DomainRef domain = domains.computeIfAbsent(domainId, id -> new DomainRef(id, stub(id)));
                DomainRef subdomain = domain.subdomains.computeIfAbsent(subdomainId, id -> new DomainRef(id, stub(id)));
                subdomain.services.put(service.id, service);
                return;
            }
            ServiceRef service = services.computeIfAbsent(serviceId, id -> new ServiceRef(id, domainId, subdomainId, stub(id)));
            String collection = parts[servicesIndex + 2];
            switch (collection) {
                case "events" -> service.events.add(page);
                case "commands" -> service.commands.add(page);
                case "queries" -> service.queries.add(page);
                case "entities" -> service.entities.add(page);
                case "flows" -> service.flows.add(page);
                case "data-products" -> service.dataProducts.add(page);
                case "diagrams" -> service.diagrams.add(page);
                default -> {
                }
            }
        }

        private int indexOf(String[] parts, String expected) {
            for (int i = 0; i < parts.length; i++) {
                if (expected.equals(parts[i])) {
                    return i;
                }
            }
            return -1;
        }

        private Page stub(String id) {
            Map<String, Object> frontmatter = new LinkedHashMap<>();
            frontmatter.put("id", id);
            frontmatter.put("name", id);
            return new Page("", frontmatter, "");
        }
    }
}
