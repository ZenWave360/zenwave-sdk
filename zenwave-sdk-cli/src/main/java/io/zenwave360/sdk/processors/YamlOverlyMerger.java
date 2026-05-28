package io.zenwave360.sdk.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class YamlOverlyMerger {
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @FunctionalInterface
    public interface ThrowingResourceLoader {
        Object apply(String resource) throws IOException;
    }

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles) {
        try {
            return mergeAndOverlay(content, mergeFile, overlayFiles, YamlOverlyMerger::getURI);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles, ThrowingResourceLoader resourceLoader) throws IOException {
        if(mergeFile != null) {
            var asyncapiAsMap = (Map) Parser.parse(content).json();
            var asyncapiMergeAsMap = parseYaml(resourceLoader.apply(mergeFile));
            var merged = YamlOverlyMerger.merge(asyncapiAsMap, (Map<String, Object>) asyncapiMergeAsMap);
            content = yamlMapper.writeValueAsString(merged);
        }
        if (overlayFiles != null && !overlayFiles.isEmpty()) {
            var asyncapiAsMap = (Map) Parser.parse(content).json();
            for (String asyncapiOverlayFile : overlayFiles) {
                var asyncapiOverlayAsMap = parseYaml(resourceLoader.apply(asyncapiOverlayFile));
                asyncapiAsMap = YamlOverlyMerger.applyOverlay(asyncapiAsMap, (Map<String, Object>) asyncapiOverlayAsMap);
            }
            content = yamlMapper.writeValueAsString(asyncapiAsMap);
        }
        return content;
    }

    private static URI getURI(String uri) {
        if(uri.startsWith("classpath:")) {
            if(!uri.toString().startsWith("classpath:/")) {
                // gracefully handle classpath: without the slash
                uri = uri.replace("classpath:", "classpath:/");
            }
            return URI.create(uri);
        }
        return new File(uri).toURI();
    }

    private static Map parseYaml(Object source) throws IOException {
        if (source instanceof URI uri) {
            return (Map) Parser.parse(uri).json();
        }
        if (source instanceof String content) {
            return (Map) Parser.parse(content).json();
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
        if (base == null || overlay == null) {
            return base;
        }

        // Create a deep copy of base to keep original untouched
        Map<String, Object> result = Maps.copy(base);

        // Process actions
        List<Map<String, Object>> actions = (List<Map<String, Object>>) overlay.get("actions");
        if (actions != null) {
            for (Map<String, Object> action : actions) {
                String target = (String) action.get("target");
                Object updateValue = action.get("update");
                Object removeValue = action.get("remove");

                if (updateValue != null) {
                    Object targetNode = JSONPath.get(result, target);
                    if(targetNode == null) {
                        // System.out.println("Target node not found: " + target);
                    } else {
                        JSONPath.set(result, target, updateValue);
                    }
                }

                if (Boolean.TRUE.equals(removeValue)) {
                    JSONPath.remove(result, target);
                }

            }
        }

        return result;
    }
}
