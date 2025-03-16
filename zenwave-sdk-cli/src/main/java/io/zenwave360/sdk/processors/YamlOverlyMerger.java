package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;

import java.util.List;
import java.util.Map;

public class YamlOverlyMerger {

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
