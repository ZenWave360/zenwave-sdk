package io.zenwave360.sdk.plugins;

import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.utils.AntStyleMatcher;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ConfigurationBuilder;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AvroSchemaLoader implements io.zenwave360.sdk.parsers.Parser {
    private Logger log = LoggerFactory.getLogger(getClass());

    public static final String AVRO_SCHEMAS_LIST = "avroSchemas";

    @DocumentedOption(description = "List of avro schema files to generate code for. It is alternative to sourceDirectory and imports.")
    public List<String> avroFiles;

    @DocumentedOption(description = "Avro Compiler Properties")
    public AvroCompilerProperties avroCompilerProperties = new AvroCompilerProperties();

    private ClassLoader projectClassLoader;

    @Override
    public Parser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        List<URI> avroFileURIs = null;
        if(avroFiles != null && !avroFiles.isEmpty()) {
            log.info("Using {} avro files: {}", avroFiles.size(), avroFiles);
            avroFileURIs = avroFiles.stream().map(URI::create).toList();
        }
        else {
            avroFileURIs = collectAvscFiles(avroCompilerProperties);
            log.debug("Found {} avsc files: {}", avroFileURIs.size(), avroFileURIs);
        }
        var avroSchemas = avroSchemasAsList(avroFileURIs);
        return Map.of(AVRO_SCHEMAS_LIST, avroSchemas);
    }

    protected List<Map<String, Object>> avroSchemasAsList(List<URI> avroFileURIs) throws IOException {
        var schemas = new ArrayList<Map<String, Object>>();
        for (URI uri : avroFileURIs) {
            $RefParser parser = new $RefParser(uri)
                    .withResourceClassLoader(this.projectClassLoader)
                    .withOptions(new $RefParserOptions().withOnCircular(SKIP).withOnMissing($RefParserOptions.OnMissing.FAIL));
            Object schema = parser.parse().getRefs().jsonContext.json();
            if(schema instanceof List) {
                schemas.addAll((List<Map<String, Object>>) schema);
            } else {
                schemas.add((Map<String, Object>) schema);
            }
        }
        return schemas;
    }

    protected List<URI> collectAvscFiles(AvroCompilerProperties avroCompilerProperties) throws IOException {
        return collectAvscFiles(avroCompilerProperties.sourceDirectory, avroCompilerProperties.imports, avroCompilerProperties.includes, avroCompilerProperties.excludes);
    }

    protected List<URI> collectAvscFiles(File sourceFolder, List<String> imports, List<String> includes, List<String> excludes) throws IOException {
        Set<File> avscFiles = new HashSet<>();
        List<URI> importedURIs = new ArrayList<>();

        // Process sourceFolder if provided
        if (sourceFolder != null && sourceFolder.exists()) {
            if (sourceFolder.isDirectory()) {
                log.info("Collecting avsc files from source folder: {}", sourceFolder);
                avscFiles.addAll(Files.walk(sourceFolder.toPath())
                        .filter(Files::isRegularFile)
                        .filter(p ->  matchesIncludes(p.toString(), includes) && !matchesExcludes(p.toString(), excludes))
                        .map(Path::toFile)
                        .toList());
            } else if (sourceFolder.isFile() && sourceFolder.getName().endsWith(".avsc")) {
                avscFiles.add(sourceFolder);
            }
        }

        // Process imports if provided
        if (imports != null) {
            log.info("Collecting avsc files from imports: {}", imports);
            for (String importPath : imports) {
                try {
                    if (importPath.startsWith("classpath:")) {
                        // Handle classpath resources (files only, not directories)
                        log.trace("Processing classpath resource: {}", importPath);
                        if (importPath.endsWith(".avsc")) {
                            importedURIs.add(URI.create(importPath));
                        } else {
                            // Scan for .avsc files within the package
                            String packagePath = importPath.replace("classpath:", "").replaceFirst("^/", "");
                            log.trace("Scanning classpath package for .avsc files: {}", packagePath);
                            try {
                                ConfigurationBuilder config = new ConfigurationBuilder()
                                        .setScanners(Scanners.Resources);

                                if (this.projectClassLoader instanceof URLClassLoader urlClassLoader) {
                                    config.addClassLoaders(urlClassLoader);
                                    config.addUrls(urlClassLoader.getURLs());
                                } else {
                                    config.forPackage(packagePath.replace("/", "."));
                                }

                                Set<String> avscResources = new Reflections(config).getResources(Pattern.compile(".*\\.avsc"));

                                // Apply package filtering as a safety net (handles current Reflections bug)
                                avscResources = avscResources.stream()
                                        .filter(resource -> resource.startsWith(packagePath))
                                        .collect(Collectors.toSet());

                                for (String resource : avscResources) {
                                    // Apply include/exclude filters to classpath resources
                                    if (matchesIncludes(resource, includes) && !matchesExcludes(resource, excludes)) {
                                        String classpathUri = "classpath:" + resource;
                                        log.trace("Found classpath .avsc file: {}", classpathUri);
                                        importedURIs.add(URI.create(classpathUri));
                                    } else {
                                        log.trace("Classpath resource {} filtered out by include/exclude patterns", resource);
                                    }
                                }

                                if (avscResources.isEmpty()) {
                                    log.warn("No .avsc files found in classpath package: {}", importPath);
                                }
                            } catch (Exception e) {
                                log.error("Error scanning classpath package '{}': {}", packagePath, e.getMessage());
                                throw new IOException("Failed to scan classpath package: " + importPath, e);
                            }
                        }
                    } else if (importPath.startsWith("https://") || importPath.startsWith("http://")) {
                        // Handle HTTPS resources
                        log.debug("Processing HTTPS resource: {}", importPath);
                        if (importPath.endsWith(".avsc")) {
                            importedURIs.add(URI.create(importPath));
                        } else {
                            log.warn("HTTPS imports only support individual .avsc files: {}", importPath);
                        }
                    } else {
                        // Handle local file system paths
                        Path path = Paths.get(importPath);
                        if (Files.isDirectory(path)) {
                            avscFiles.addAll(Files.walk(path)
                                    .filter(Files::isRegularFile)
                                    .filter(p -> matchesIncludes(p.toString(), includes) && !matchesExcludes(p.toString(), excludes))
                                    .map(Path::toFile)
                                    .toList());
                        } else if (Files.isRegularFile(path) && importPath.endsWith(".avsc")) {
                            if (matchesIncludes(path.toString(), includes) && !matchesExcludes(path.toString(), excludes)) {
                                avscFiles.add(path.toFile());
                            }
                        } else {
                            log.warn("Skipping invalid import path: {}", importPath);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing import path '{}': {}", importPath, e.getMessage());
                    throw new IOException("Failed to process import: " + importPath, e);
                }
            }
            log.info("Found {} avsc files in imports: {}", importedURIs.size(), importedURIs);
        }

        // Combine local file URIs with remote URIs
        List<URI> allURIs = new ArrayList<>();
        allURIs.addAll(avscFiles.stream().map(File::toURI).toList());
        allURIs.addAll(importedURIs);

        return allURIs;
    }

    private boolean matchesIncludes(String pathString, List<String> includes) {
        if (includes == null || includes.isEmpty()) {
            boolean matches = pathString.endsWith(".avsc");
            log.trace("File {} matches default include pattern: {}", pathString, matches);
            return matches;
        }
        String normalizedPath = pathString.replace("\\", "/");
        boolean matches = includes.stream().anyMatch(include -> AntStyleMatcher.match(include, normalizedPath));
        log.trace("File {} matches includes {}: {}", normalizedPath, includes, matches);
        return matches;
    }

    private boolean matchesExcludes(String pathString, List<String> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            log.trace("File {} has no excludes, not excluded", pathString);
            return false;
        }
        String normalizedPath = pathString.replace("\\", "/");
        boolean matches = excludes.stream().anyMatch(exclude -> AntStyleMatcher.match(exclude, normalizedPath));
        log.trace("File {} matches excludes {}: {}", normalizedPath, excludes, matches);
        return matches;
    }

}
