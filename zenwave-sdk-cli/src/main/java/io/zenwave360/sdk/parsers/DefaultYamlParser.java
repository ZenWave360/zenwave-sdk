package io.zenwave360.sdk.parsers;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.jsonrefparser.JavaRefParser;
import io.zenwave360.jsonrefparser.$Refs;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;
import io.zenwave360.jsonrefparser.$RefParserOptions.OnMissing;
import io.zenwave360.jsonrefparser.model.OnCircular;
import io.zenwave360.jsonrefparser.model.RefParserOptions;
import io.zenwave360.sdk.processors.YamlOverlyMerger;
import org.apache.commons.lang3.StringUtils;
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

    @Override
    public Map<String, Object> parse() throws IOException {
        Map<String, Object> model = new LinkedHashMap<>();
        if(apiFile != null) {
            if (apiOverlayFiles != null && !apiOverlayFiles.isEmpty()) {
                model.put(targetProperty, parseWithOverlays());
            } else {
                $RefParser parser = new $RefParser(apiFile)
                        .withResourceClassLoader(this.projectClassLoader)
                        .withAuthenticationValues(authentication)
                        .withOptions(new $RefParserOptions().withOnCircular(SKIP).withOnMissing(OnMissing.SKIP));
                model.put(targetProperty, new Model(apiFile, parser.parse().dereference().mergeAllOf().getRefs()));
            }
        } else {
            log.error("No API Specification (apiFile) provided");
        }
        return model;
    }

    protected Model parseWithOverlays() throws IOException {
        String baseContent = loadUriContent(apiFile);
        String overlayedContent = YamlOverlyMerger.mergeAndOverlay(baseContent, null, apiOverlayFiles, this::loadUriContent);
        URI baseUri = normalizeBaseUri(apiFile);
        JavaRefParser parser = JavaRefParser.fromText(overlayedContent, baseUri.toString())
                .withResourceClassLoader(this.projectClassLoader)
                .withAuthentication(authentication.toArray(AuthenticationValue[]::new))
                .withOptions(new RefParserOptions(OnCircular.SKIP, io.zenwave360.jsonrefparser.model.OnMissing.SKIP));
        return new Model(apiFile, $Refs.from(parser.parse().dereference().mergeAllOf().getParsedDocument()));
    }

    protected String loadUriContent(URI uri) throws IOException {
        if ("classpath".equalsIgnoreCase(uri.getScheme())) {
            String resource = getClasspathResourcePath(uri);
            ClassLoader resourceClassLoader = projectClassLoader != null ? projectClassLoader : getClass().getClassLoader();
            try (InputStream inputStream = resourceClassLoader.getResourceAsStream(resource)) {
                if (inputStream == null) {
                    throw new IOException("InputStream not found for " + uri);
                }
                return readString(inputStream);
            }
        }
        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            return loadRemoteUriContent(uri);
        }
        if (uri.getScheme() == null || uri.getScheme().length() == 1) {
            return Files.readString(Path.of(uri.toString()), StandardCharsets.UTF_8);
        }
        return Files.readString(new File(uri).toPath(), StandardCharsets.UTF_8);
    }

    protected String loadUriContent(String uri) throws IOException {
        return loadUriContent(normalizeInputUri(uri));
    }

    private String loadRemoteUriContent(URI uri) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        for (AuthenticationValue authValue : authentication) {
            if (authValue.getType() == AuthenticationValue.AuthenticationType.HEADER && authValue.matches(uri.toURL())) {
                connection.setRequestProperty(authValue.getKey(), authValue.getValue());
            }
        }
        connection.setRequestProperty("Accept", "application/json, application/yaml, */*");
        connection.setRequestProperty("User-Agent", "zenwave-sdk");
        try (InputStream inputStream = connection.getInputStream()) {
            return readString(inputStream);
        }
    }

    private String readString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private String getClasspathResourcePath(URI uri) {
        String resource = uri.getPath();
        if (resource == null) {
            resource = uri.getSchemeSpecificPart();
        }
        return StringUtils.removeStart(resource, "/");
    }

    private URI normalizeBaseUri(URI uri) {
        if ("classpath".equalsIgnoreCase(uri.getScheme()) && !uri.toString().startsWith("classpath:/")) {
            return URI.create(uri.toString().replace("classpath:", "classpath:/"));
        }
        if (uri.getScheme() == null || uri.getScheme().length() == 1) {
            return new File(uri.toString()).toURI();
        }
        return uri;
    }

    private URI normalizeInputUri(String uri) {
        if (uri.startsWith("classpath:")) {
            return URI.create(uri.startsWith("classpath:/")
                    ? uri
                    : uri.replace("classpath:", "classpath:/"));
        }
        if (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("file:")) {
            return URI.create(uri);
        }
        return new File(uri).toURI();
    }
}
