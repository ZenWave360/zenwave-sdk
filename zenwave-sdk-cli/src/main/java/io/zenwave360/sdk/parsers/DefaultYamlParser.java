package io.zenwave360.sdk.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.jsonrefparser.JavaRefParser;
import io.zenwave360.jsonrefparser.$Refs;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.jsonrefparser.model.OnCircular;
import io.zenwave360.jsonrefparser.model.RefParserOptions;
import io.zenwave360.sdk.processors.YamlOverlyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultYamlParser implements io.zenwave360.sdk.parsers.Parser {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "API Specification File")
    public URI apiFile;
    public String targetProperty = "api";

    @DocumentedOption(description = "Authentication configuration values for fetching remote resources.")
    public List<AuthenticationValue> authentication = List.of();

    @DocumentedOption(description = "Ordered list of API overlay YAML files applied before dereferencing and allOf merge.")
    public List<String> apiOverlayFiles = List.of();

    @Deprecated
    @DocumentedOption(description = "Deprecated alias for apiOverlayFiles.")
    public void setAsyncapiOverlayFiles(List<String> asyncapiOverlayFiles) {
        this.apiOverlayFiles = asyncapiOverlayFiles;
    }

    private ClassLoader projectClassLoader;

    private SpecResourceLoader resourceLoader() {
        return new SpecResourceLoader()
                .withProjectClassLoader(projectClassLoader)
                .withAuthentication(authentication);
    }

    @DocumentedOption(description = "API Specification File (@deprecated use apiFile)")
    public void setSpecFile(URI specFile) {
        apiFile = specFile;
    }

    @DocumentedOption(description = "API Specification File (@deprecated use apiFile)")
    public void setOpenapiFile(URI openapiFile) {
        apiFile = openapiFile;
    }


    @Override
    public DefaultYamlParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public DefaultYamlParser withApiFile(URI specFile) {
        this.apiFile = specFile;
        return this;
    }

    public DefaultYamlParser withApiFile(File specFile) {
        this.apiFile = specFile.getAbsoluteFile().toURI();
        return this;
    }

    public DefaultYamlParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    public DefaultYamlParser withAuthentication(List<AuthenticationValue> authentication) {
        this.authentication = authentication != null ? authentication : List.of();
        return this;
    }

    public String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles) throws IOException {
        return YamlOverlyMerger.mergeAndOverlay(content, mergeFile, overlayFiles, this::loadUriContent);
    }

    public String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles,
                                  UnaryOperator<Map<String, Object>> documentOrderer) throws IOException {
        return YamlOverlyMerger.mergeAndOverlay(content, mergeFile, overlayFiles, this::loadUriContent, documentOrderer);
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        Map<String, Object> model = new LinkedHashMap<>();
        if(apiFile != null) {
            if (apiOverlayFiles != null && !apiOverlayFiles.isEmpty()) {
                model.put(targetProperty, parseWithOverlays());
            } else {
                model.put(targetProperty, resourceLoader().parse(apiFile));
            }
        } else {
            log.error("No API Specification (apiFile) provided");
        }
        return model;
    }

    protected Model parseWithOverlays() throws IOException {
        String baseContent = loadUriContent(apiFile);
        String overlayedContent = mergeAndOverlay(baseContent, null, apiOverlayFiles);
        URI baseUri = resourceLoader().normalizeBaseUri(apiFile);
        JavaRefParser parser = JavaRefParser.fromText(overlayedContent, baseUri.toString())
                .withResourceClassLoader(this.projectClassLoader)
                .withAuthentication(authentication.toArray(AuthenticationValue[]::new))
                .withOptions(new RefParserOptions(OnCircular.SKIP, io.zenwave360.jsonrefparser.model.OnMissing.SKIP));
        return new Model(apiFile, $Refs.from(parser.parse().dereference().mergeAllOf().getParsedDocument()));
    }

    protected String loadUriContent(URI uri) throws IOException {
        return resourceLoader().load(uri);
    }

    protected String loadUriContent(String uri) throws IOException {
        return resourceLoader().load(uri);
    }
}
