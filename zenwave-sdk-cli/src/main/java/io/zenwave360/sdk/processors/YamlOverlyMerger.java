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

    public static String mergeAndOverlay(String content, String mergeFile, List<String> overlayFiles) {
        if(mergeFile != null) {
            try {
                var asyncapiAsMap = (Map) Parser.parse(content).json();
                var asyncapiMergeAsMap = (Map) Parser.parse(getURI(mergeFile)).json();
                var merged = YamlOverlyMerger.merge(asyncapiAsMap, (Map<String, Object>) asyncapiMergeAsMap);
                content = yamlMapper.writeValueAsString(merged);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (overlayFiles != null && !overlayFiles.isEmpty()) {
            try {
                var asyncapiAsMap = (Map) Parser.parse(content).json();
                for (String asyncapiOverlayFile : overlayFiles) {
                    var asyncapiOverlayAsMap = (Map) Parser.parse(getURI(asyncapiOverlayFile)).json();
                    asyncapiAsMap = YamlOverlyMerger.applyOverlay(asyncapiAsMap, (Map<String, Object>) asyncapiOverlayAsMap);
                }
                content = yamlMapper.writeValueAsString(asyncapiAsMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
                        System.out.println("Target node not found: " + target);
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
