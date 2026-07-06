package io.zenwave360.sdk.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.zenwave360.jsonrefparser.JavaRefParser;
import io.zenwave360.sdk.utils.Maps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class YamlOverlyMerger {
    private static final ObjectMapper yamlMapper = new ObjectMapper(YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .build());

    @FunctionalInterface
    public interface ThrowingResourceLoader {
        Object apply(String resource) throws IOException;
    }

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles) {
        return mergeAndOverlayWithOrderer(content, mergeFile, overlayFiles, UnaryOperator.identity());
    }

    public static String mergeAndOverlayWithOrderer(String content, String mergeFile, List<String> overlayFiles,
                                                    UnaryOperator<Map<String, Object>> documentOrderer) {
        try {
            return mergeAndOverlay(content, mergeFile, overlayFiles, YamlOverlyMerger::getURI, documentOrderer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles, ThrowingResourceLoader resourceLoader) throws IOException {
        return mergeAndOverlay(content, mergeFile, overlayFiles, resourceLoader, UnaryOperator.identity());
    }

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles,
                                         ThrowingResourceLoader resourceLoader,
                                         UnaryOperator<Map<String, Object>> documentOrderer) throws IOException {
        if(mergeFile != null) {
            var asyncapiAsMap = parseYaml(content);
            var asyncapiMergeAsMap = parseYaml(resourceLoader.apply(mergeFile));
            var merged = YamlOverlyMerger.merge(asyncapiAsMap, (Map<String, Object>) asyncapiMergeAsMap);
            content = writeYaml(merged, documentOrderer);
        }
        if (overlayFiles != null && !overlayFiles.isEmpty()) {
            var asyncapiAsMap = parseYaml(content);
            for (String asyncapiOverlayFile : overlayFiles) {
                var asyncapiOverlayAsMap = parseYaml(resourceLoader.apply(asyncapiOverlayFile));
                asyncapiAsMap = YamlOverlyMerger.applyOverlay(asyncapiAsMap, (Map<String, Object>) asyncapiOverlayAsMap);
            }
            content = writeYaml(asyncapiAsMap, documentOrderer);
        }
        return content;
    }

    private static String writeYaml(Map<String, Object> document,
                                    UnaryOperator<Map<String, Object>> documentOrderer) throws IOException {
        Map<String, Object> orderedDocument = documentOrderer.apply(document);
        return addSpacingBetweenRootElements(yamlMapper.writeValueAsString(orderedDocument));
    }

    private static String addSpacingBetweenRootElements(String yaml) {
        StringBuilder formatted = new StringBuilder(yaml.length());
        int rootElementCount = 0;
        boolean firstRootElementWasOneLine = false;
        boolean previousLineWasEmpty = false;

        for (String line : yaml.split("\\R")) {
            int separatorIndex = line.indexOf(':');
            boolean rootElement = !line.isEmpty()
                    && !Character.isWhitespace(line.charAt(0))
                    && !line.startsWith("---")
                    && separatorIndex > 0;
            boolean followsOneLineFirstElement = rootElementCount == 1 && firstRootElementWasOneLine;
            if (rootElement && rootElementCount > 0 && !previousLineWasEmpty && !followsOneLineFirstElement) {
                formatted.append('\n');
            }
            formatted.append(line).append('\n');
            previousLineWasEmpty = line.isEmpty();
            if (rootElement) {
                if (rootElementCount == 0) {
                    firstRootElementWasOneLine = !line.substring(separatorIndex + 1).isBlank();
                }
                rootElementCount++;
            }
        }

        return formatted.toString();
    }

    private static URI getURI(String uri) {
        if(uri.startsWith("classpath:")) {
            if(!uri.toString().startsWith("classpath:/")) {
                // gracefully handle classpath: without the slash
                uri = uri.replace("classpath:", "classpath:/");
            }
            return URI.create(uri);
        }
        if(uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("file:")) {
            return URI.create(uri);
        }
        return new File(uri).toURI();
    }

    private static Map parseYaml(Object source) throws IOException {
        if (source instanceof URI uri) {
            return JavaRefParser.from(uri).parse().getParsedDocument().getSchema();
        }
        if (source instanceof String content) {
            return JavaRefParser.fromText(content).parse().getParsedDocument().getSchema();
        }
        throw new IllegalArgumentException("Unsupported YAML source: " + source);
    }

    public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> merger) {
        if (base == null || merger == null) {
            return base;
        }
        // Create a deep copy of base to keep original untouched
        Map<String, Object> result = Maps.copy(base);
        // Perform deep merge of merger into the copy
        return Maps.deepMerge(result, merger);
    }

    public static Map<String, Object> applyOverlay(Map<String, Object> base, Map<String, Object> overlay) {
        return OverlayApplicator.apply(base, overlay);
    }
}
