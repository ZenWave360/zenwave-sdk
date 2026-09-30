package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.language.zdl.ZdlParser;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.plugins.ConfigurationProvider;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZDLParser implements Parser, ConfigurationProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob", "byte");
    public static final String REFERENCED_API_MODEL_PROPERTY = "model";
    /** Set on an {@code apis} entry, when {@code continueOnZdlError} is on, with the reason its model failed to load. */
    public static final String REFERENCED_API_ERROR_PROPERTY = "modelLoadError";

    public List<String> zdlFiles = List.of();
    private String content;
    public String targetProperty = "zdl";

    @DocumentedOption(description = "Continue even when ZDL contains fatal errors")
    public boolean continueOnZdlError = true;

    public Map<String, String> options = new HashMap<>();

    @DocumentedOption(description = "Authentication configuration values for fetching remote resources.")
    public List<AuthenticationValue> authentication = List.of();

    private ClassLoader projectClassLoader;

    public void setZdlFile(String zdlFile) {
        if(zdlFile != null) {
            this.zdlFiles = List.of(zdlFile);
        }
    }

    /**
     * @deprecated Parsing several files joined into one model is deprecated: one run parses one ZDL file.
     *             Reference other models with {@code apis { zdl ... }} instead.
     */
    @Deprecated
    public void setZdlFiles(List<String> zdlFiles) {
        this.zdlFiles = zdlFiles;
    }

    public ZDLParser withContent(String content) {
        this.content = content;
        return this;
    }

    public ZDLParser withZdlFile(String zdlFile) {
        this.zdlFiles = List.of(zdlFile);
        return this;
    }

    public ZDLParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    public ZDLParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public ZDLParser withAuthentication(List<AuthenticationValue> authentication) {
        this.authentication = authentication != null ? authentication : List.of();
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        String zdlString = content;
        if(zdlString == null) {
            SpecResourceLoader loader = new SpecResourceLoader()
                    .withProjectClassLoader(projectClassLoader)
                    .withAuthentication(authentication);
            if (zdlFiles.size() > 1) {
                System.err.println("ZDL WARNING: joining several ZDL files into one model is deprecated "
                        + "and will be removed; parse one file and reference others with 'apis { zdl ... }': " + zdlFiles);
            }
            StringBuilder zdlContent = new StringBuilder();
            for (String zdlFile : zdlFiles) {
                zdlContent.append(loader.load(zdlFile));
            }
            zdlString = zdlContent.toString();
        }
        Map zdlModel = new ZdlParser().parseModel(zdlString);
        var problems = JSONPath.get(zdlModel, "$.problems", List.of());
        if(!problems.isEmpty()) {
            for (Object problem : problems) {
                var message = JSONPath.get(problem, "message");
                var location = JSONPath.get(problem, "location", new int[5]);
                var path = JSONPath.get(problem, "path");
                System.err.printf("ZDL ERROR [%s]: %s [line: %s, char: %s]%n", path, message, location[2], location[3]+1);
            }
            if(!continueOnZdlError) {
                throw new ParseProblemsException(problems);
            }
        }
        loadReferencedApiModels(zdlModel, declaringDocumentUri());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put(targetProperty, zdlModel);
        return model;
    }

    public static Model getReferencedApiModel(Map<String, Object> api) {
        var model = api.get(REFERENCED_API_MODEL_PROPERTY);
        return model instanceof Model ? (Model) model : null;
    }

    public static Map<String, Object> getReferencedZdlModel(Map<String, Object> api) {
        var model = api.get(REFERENCED_API_MODEL_PROPERTY);
        return model instanceof Model ? null : (Map<String, Object>) model;
    }

    private void loadReferencedApiModels(Map<String, Object> zdlModel, URI declaringDocument) throws IOException {
        Map<String, Map<String, Object>> apis = JSONPath.get(zdlModel, "$.apis", Map.of());
        SpecResourceLoader loader = new SpecResourceLoader()
                .withProjectClassLoader(projectClassLoader)
                .withAuthentication(authentication);
        for (Map<String, Object> api : apis.values()) {
            String apiUri = apiUri(api);
            if (apiUri == null || apiUri.isBlank()) {
                continue;
            }
            URI resolvedApiUri = null;
            try {
                resolvedApiUri = loader.resolve(apiUri, declaringDocument);
                api.put(REFERENCED_API_MODEL_PROPERTY, parseReferencedApi(api, resolvedApiUri, loader));
            } catch (Exception e) {
                if (!continueOnZdlError) {
                    throw e instanceof IOException ioException ? ioException : new IOException(e.getMessage(), e);
                }
                api.put(REFERENCED_API_ERROR_PROPERTY, rootMessage(e));
                log.warn("Unable to load referenced API '{}' from {}: {}", api.get("name"),
                        resolvedApiUri != null ? resolvedApiUri : apiUri, e.getMessage());
            }
        }
    }

    /**
     * Referenced documents are parsed by their declared type: {@code zdl} references with the ZDL parser
     * (as a separate model map, never concatenated with the primary model and without loading their own
     * {@code apis {}} references), everything else as YAML with URI-relative {@code $ref} resolution.
     */
    private Object parseReferencedApi(Map<String, Object> api, URI resolvedApiUri, SpecResourceLoader loader) throws IOException {
        if ("zdl".equals(api.get("type"))) {
            // Intentionally keep referenced ZDLs as independent raw parser models. They are not run
            // through ZDLProcessor, so consumers must only rely on raw declarations such as config
            // and events unless this loading boundary is explicitly expanded in the future.
            Map<String, Object> referencedZdlModel = new ZdlParser().parseModel(loader.load(resolvedApiUri));
            var problems = JSONPath.get(referencedZdlModel, "$.problems", List.of());
            if (!problems.isEmpty()) {
                // Same policy as the primary model: report problems, and only fail when continueOnZdlError is off.
                for (Object problem : problems) {
                    System.err.printf("ZDL ERROR [%s] in referenced '%s' (%s): %s%n", JSONPath.get(problem, "path"),
                            api.get("name"), resolvedApiUri, JSONPath.get(problem, "message"));
                }
                if (!continueOnZdlError) {
                    throw new IOException("Referenced ZDL '" + api.get("name") + "' has " + problems.size() + " parse problems");
                }
            }
            return referencedZdlModel;
        }
        return loader.parse(resolvedApiUri);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }

    private String apiUri(Map<String, Object> api) {
        Object uri = api.get("uri");
        if (uri instanceof String) {
            return (String) uri;
        }
        return JSONPath.get(api, "$.config.uri", (String) null);
    }

    private URI declaringDocumentUri() {
        if (content == null && !zdlFiles.isEmpty()) {
            return new SpecResourceLoader().toUri(zdlFiles.get(0));
        }
        return null;
    }

    @Override
    public void updateConfiguration(Plugin configuration, Map<String, Object> model) {
        var zdl = model.get(targetProperty);
        var config = JSONPath.get(zdl, "$.config", Map.<String, Object>of());

        if (config != null) {
            for (var entry : config.entrySet()) {
                if(!configuration.getOptions().containsKey(entry.getKey())) {
                    configuration.withOption(entry.getKey(), entry.getValue());
                }
            }
            if(config.containsKey("layout")) {
                configuration.withLayout((String) config.get("layout"));
                configuration.getProcessedLayout();
            }
        }
    }
}
