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
import java.util.List;
import java.util.stream.Collectors;

import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;
import io.zenwave360.jsonrefparser.$RefParserOptions.OnMissing;
import io.zenwave360.jsonrefparser.AuthenticationValue;
import org.apache.commons.lang3.Strings;

/**
 * Loads and parses specification resources while preserving their URI as the base
 * for relative references.
 */
public class SpecResourceLoader {

    private ClassLoader projectClassLoader;
    private List<AuthenticationValue> authentication = List.of();

    public SpecResourceLoader withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public SpecResourceLoader withAuthentication(List<AuthenticationValue> authentication) {
        this.authentication = authentication != null ? authentication : List.of();
        return this;
    }

    public Model parse(String resource) throws IOException {
        return parse(toUri(resource));
    }

    public Model parse(URI resource) throws IOException {
        URI normalizedResource = normalizeBaseUri(resource);
        $RefParser parser = new $RefParser(normalizedResource)
                .withResourceClassLoader(projectClassLoader)
                .withAuthenticationValues(authentication)
                .withOptions(new $RefParserOptions().withOnCircular(SKIP).withOnMissing(OnMissing.SKIP));
        return new Model(resource, parser.parse().dereference().mergeAllOf().getRefs());
    }

    public String load(String resource) throws IOException {
        return load(toUri(resource));
    }

    public String load(URI resource) throws IOException {
        URI normalizedResource = normalizeBaseUri(resource);
        if ("classpath".equalsIgnoreCase(normalizedResource.getScheme())) {
            String resourcePath = getClasspathResourcePath(normalizedResource);
            ClassLoader resourceClassLoader = projectClassLoader != null
                    ? projectClassLoader
                    : getClass().getClassLoader();
            try (InputStream inputStream = resourceClassLoader.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new IOException("InputStream not found for " + resource);
                }
                return readString(inputStream);
            }
        }
        if (isHttp(normalizedResource)) {
            return loadRemote(normalizedResource);
        }
        return Files.readString(Path.of(normalizedResource), StandardCharsets.UTF_8);
    }

    /** Resolve a resource reference relative to the document that declares it. */
    public URI resolve(String resource, URI declaringDocument) {
        URI resourceUri = rawUri(resource);
        if (resourceUri.isAbsolute() || declaringDocument == null || isWindowsAbsolutePath(resource)) {
            return toUri(resource);
        }
        return normalizeBaseUri(declaringDocument).resolve(resourceUri).normalize();
    }

    public URI toUri(String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        if (resource.startsWith("classpath:")) {
            return normalizeBaseUri(URI.create(resource));
        }
        if (resource.startsWith("http://") || resource.startsWith("https://") || resource.startsWith("file:")) {
            return URI.create(resource);
        }
        return new File(resource).getAbsoluteFile().toURI();
    }

    public URI normalizeBaseUri(URI resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        if ("classpath".equalsIgnoreCase(resource.getScheme()) && !resource.toString().startsWith("classpath:/")) {
            return URI.create(resource.toString().replace("classpath:", "classpath:/"));
        }
        if (resource.getScheme() == null || resource.getScheme().length() == 1) {
            return new File(resource.toString()).getAbsoluteFile().toURI();
        }
        return resource;
    }

    private URI rawUri(String resource) {
        try {
            return URI.create(resource.replace('\\', '/').replace(" ", "%20"));
        } catch (IllegalArgumentException e) {
            return new File(resource).toURI();
        }
    }

    private boolean isWindowsAbsolutePath(String resource) {
        return resource.length() > 2
                && Character.isLetter(resource.charAt(0))
                && resource.charAt(1) == ':'
                && (resource.charAt(2) == '/' || resource.charAt(2) == '\\');
    }

    private String loadRemote(URI resource) throws IOException {
        URLConnection connection = resource.toURL().openConnection();
        for (AuthenticationValue authValue : authentication) {
            if (authValue.getType() == AuthenticationValue.AuthenticationType.HEADER
                    && authValue.matches(resource.toURL())) {
                connection.setRequestProperty(authValue.getKey(), authValue.getValue());
            }
        }
        connection.setRequestProperty("Accept", "application/json, application/yaml, */*");
        connection.setRequestProperty("User-Agent", "zenwave-sdk");
        try (InputStream inputStream = connection.getInputStream()) {
            return readString(inputStream);
        }
    }

    private boolean isHttp(URI resource) {
        return "http".equalsIgnoreCase(resource.getScheme()) || "https".equalsIgnoreCase(resource.getScheme());
    }

    private String readString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private String getClasspathResourcePath(URI resource) {
        String resourcePath = resource.getPath();
        if (resourcePath == null) {
            resourcePath = resource.getSchemeSpecificPart();
        }
        return Strings.CS.removeStart(resourcePath, "/");
    }
}
