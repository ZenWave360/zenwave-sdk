package io.zenwave360.sdk.processors;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import io.zenwave360.sdk.utils.Maps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies Overlay 1.0 and 1.1 actions using the SDK's existing Jayway JSONPath implementation.
 *
 * <p>Jayway predates RFC 9535 and uses its own JSONPath dialect. Overlay 1.1 expressions are
 * therefore supported on a best-effort basis and must not be described as fully RFC 9535
 * compliant.</p>
 */
final class OverlayApplicator {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Configuration PATH_LIST_CONFIGURATION = Configuration.builder()
            .options(Option.AS_PATH_LIST)
            .build();

    private OverlayApplicator() {
    }

    static Map<String, Object> apply(Map<String, Object> base, Map<String, Object> overlay) {
        if (base == null || overlay == null) {
            return base;
        }

        FeatureSet featureSet = parseAndValidateFeatureSet(overlay);
        List<Map<String, Object>> actions = validateActions(overlay, featureSet);
        Map<String, Object> result = deepCopyMap(base);

        for (int index = 0; index < actions.size(); index++) {
            applyAction(result, actions.get(index), featureSet, index);
        }
        return result;
    }

    private static FeatureSet parseAndValidateFeatureSet(Map<String, Object> overlay) {
        Object versionValue = overlay.get("overlay");
        if (!(versionValue instanceof String version)) {
            throw new IllegalArgumentException("Overlay field 'overlay' is required and must be a version string");
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported overlay version '" + version + "': expected major.minor.patch");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        FeatureSet featureSet;
        if (major == 1 && minor == 0) {
            featureSet = FeatureSet.V1_0;
        } else if (major == 1 && minor == 1) {
            featureSet = FeatureSet.V1_1;
        } else {
            throw new IllegalArgumentException("Unsupported Overlay feature set " + major + "." + minor);
        }

        Object infoValue = overlay.get("info");
        if (!(infoValue instanceof Map<?, ?> info)) {
            throw new IllegalArgumentException("Overlay field 'info' is required and must be an object");
        }
        requireString(info, "title", "Overlay info");
        requireString(info, "version", "Overlay info");
        if (info.containsKey("description") && !(info.get("description") instanceof String)) {
            throw new IllegalArgumentException("Overlay info field 'description' must be a string");
        }

        Object actionsValue = overlay.get("actions");
        if (!(actionsValue instanceof List<?> actions) || actions.isEmpty()) {
            throw new IllegalArgumentException("Overlay field 'actions' is required and must be a non-empty array");
        }
        return featureSet;
    }

    private static List<Map<String, Object>> validateActions(Map<String, Object> overlay, FeatureSet featureSet) {
        List<?> rawActions = (List<?>) overlay.get("actions");
        List<Map<String, Object>> actions = new ArrayList<>(rawActions.size());
        for (int index = 0; index < rawActions.size(); index++) {
            Object rawAction = rawActions.get(index);
            if (!(rawAction instanceof Map<?, ?> action)) {
                throw actionError(index, "must be an object");
            }

            requireString(action, "target", "Overlay action " + index);
            if (action.containsKey("description") && !(action.get("description") instanceof String)) {
                throw actionError(index, "field 'description' must be a string");
            }
            if (action.containsKey("remove") && !(action.get("remove") instanceof Boolean)) {
                throw actionError(index, "field 'remove' must be a boolean");
            }
            if (action.containsKey("copy")) {
                if (featureSet == FeatureSet.V1_0) {
                    throw actionError(index, "field 'copy' requires Overlay 1.1");
                }
                if (!(action.get("copy") instanceof String)) {
                    throw actionError(index, "field 'copy' must be a JSONPath string");
                }
            }

            boolean removes = Boolean.TRUE.equals(action.get("remove"));
            boolean updates = action.containsKey("update");
            boolean copies = action.containsKey("copy");
            if (!removes && !updates && !copies) {
                throw actionError(index, "must define update, copy, or remove: true");
            }
            actions.add((Map<String, Object>) action);
        }
        return actions;
    }

    private static void applyAction(Map<String, Object> result, Map<String, Object> action,
                                    FeatureSet featureSet, int actionIndex) {
        String target = (String) action.get("target");
        List<NodeReference> targets = selectNodes(result, target, "target", actionIndex);

        if (featureSet == FeatureSet.V1_0) {
            validateLegacyTargets(targets, actionIndex);
        }
        if (targets.isEmpty()) {
            return;
        }

        if (Boolean.TRUE.equals(action.get("remove"))) {
            removeTargets(result, targets, actionIndex);
        } else if (action.containsKey("update")) {
            Object update = action.get("update");
            if (featureSet == FeatureSet.V1_0) {
                applyLegacyUpdate(result, targets, update);
            } else {
                applyUpdate(result, targets, update, actionIndex);
            }
        } else {
            String copyExpression = (String) action.get("copy");
            List<NodeReference> sources = selectNodes(result, copyExpression, "copy", actionIndex);
            if (sources.size() != 1) {
                throw actionError(actionIndex,
                        "copy expression must select exactly one node but selected " + sources.size());
            }
            applyUpdate(result, targets, sources.get(0).value(), actionIndex);
        }
    }

    private static void validateLegacyTargets(List<NodeReference> targets, int actionIndex) {
        for (NodeReference target : targets) {
            if (target.category() == NodeCategory.PRIMITIVE) {
                throw actionError(actionIndex, "Overlay 1.0 targets must select only objects or arrays");
            }
        }
    }

    private static void applyLegacyUpdate(Map<String, Object> result, List<NodeReference> targets, Object update) {
        for (NodeReference target : targets) {
            if (target.value() instanceof Map<?, ?> targetMap && update instanceof Map<?, ?> updateMap) {
                Maps.deepMerge((Map) targetMap, deepCopyMap((Map<?, ?>) updateMap));
            } else {
                setValue(result, target.path(), deepCopy(update));
            }
        }
    }

    private static void applyUpdate(Map<String, Object> result, List<NodeReference> targets,
                                    Object update, int actionIndex) {
        validateHomogeneousTargets(targets, actionIndex);
        for (NodeReference target : targets) {
            switch (target.category()) {
                case OBJECT -> {
                    if (!(update instanceof Map<?, ?> updateMap)) {
                        throw incompatibleTypes(actionIndex, target.value(), update);
                    }
                    mergeObjects((Map<String, Object>) target.value(), updateMap, actionIndex);
                }
                case ARRAY -> {
                    List<Object> targetArray = (List<Object>) target.value();
                    if (update instanceof List<?> updateArray) {
                        for (Object item : updateArray) {
                            targetArray.add(deepCopy(item));
                        }
                    } else {
                        targetArray.add(deepCopy(update));
                    }
                }
                case PRIMITIVE -> {
                    if (categoryOf(update) != NodeCategory.PRIMITIVE) {
                        throw incompatibleTypes(actionIndex, target.value(), update);
                    }
                    setValue(result, target.path(), deepCopy(update));
                }
            }
        }
    }

    private static void mergeObjects(Map<String, Object> target, Map<?, ?> update, int actionIndex) {
        for (Map.Entry<?, ?> entry : update.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw actionError(actionIndex, "update object keys must be strings");
            }
            Object updateValue = entry.getValue();
            if (!target.containsKey(key)) {
                target.put(key, deepCopy(updateValue));
                continue;
            }

            Object targetValue = target.get(key);
            NodeCategory targetCategory = categoryOf(targetValue);
            NodeCategory updateCategory = categoryOf(updateValue);
            if (targetCategory != updateCategory) {
                throw incompatiblePropertyTypes(actionIndex, key, targetValue, updateValue);
            }

            switch (targetCategory) {
                case OBJECT -> mergeObjects((Map<String, Object>) targetValue, (Map<?, ?>) updateValue, actionIndex);
                case ARRAY -> {
                    List<Object> targetArray = (List<Object>) targetValue;
                    for (Object item : (List<?>) updateValue) {
                        targetArray.add(deepCopy(item));
                    }
                }
                case PRIMITIVE -> target.put(key, deepCopy(updateValue));
            }
        }
    }

    private static void validateHomogeneousTargets(List<NodeReference> targets, int actionIndex) {
        if (targets.size() < 2) {
            return;
        }
        NodeCategory category = targets.get(0).category();
        if (targets.stream().anyMatch(target -> target.category() != category)) {
            throw actionError(actionIndex, "update and copy targets must all be objects, arrays, or primitives");
        }
    }

    private static void removeTargets(Map<String, Object> result, List<NodeReference> targets, int actionIndex) {
        if (targets.stream().anyMatch(target -> "$".equals(target.path()))) {
            throw actionError(actionIndex, "the document root cannot be removed");
        }

        List<NodeReference> orderedTargets = new ArrayList<>(targets);
        orderedTargets.sort(Comparator
                .comparingInt(NodeReference::depth).reversed()
                .thenComparing(NodeReference::parentPath)
                .thenComparing(NodeReference::arrayIndex,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NodeReference::path));

        for (NodeReference target : orderedTargets) {
            try {
                JsonPath.parse(result).delete(target.path());
            } catch (PathNotFoundException ignored) {
                // An ancestor selected by the same action may already have removed this node.
            }
        }
    }

    private static List<NodeReference> selectNodes(Map<String, Object> document, String expression,
                                                   String expressionType, int actionIndex) {
        try {
            List<String> paths = JsonPath.using(PATH_LIST_CONFIGURATION).parse(document).read(expression);
            List<String> concretePaths = expandUnionPaths(paths);
            List<NodeReference> nodes = new ArrayList<>(concretePaths.size());
            for (String path : concretePaths) {
                Object value = JsonPath.read(document, path);
                nodes.add(NodeReference.from(path, value));
            }
            return nodes;
        } catch (PathNotFoundException ignored) {
            return List.of();
        } catch (InvalidPathException | ClassCastException exception) {
            throw actionError(actionIndex,
                    "invalid " + expressionType + " JSONPath '" + expression + "': " + exception.getMessage());
        }
    }

    /**
     * Jayway keeps multi-selector unions as one path in AS_PATH_LIST mode. Overlay actions operate
     * on nodes, so expand those retained unions into their concrete member/index paths.
     */
    private static List<String> expandUnionPaths(List<String> paths) {
        List<String> expanded = new ArrayList<>();
        for (String path : paths) {
            List<String> partialPaths = new ArrayList<>(List.of("$"));
            int index = 1;
            while (index < path.length()) {
                if (path.charAt(index) != '[') {
                    throw new IllegalArgumentException("Unexpected normalized JSONPath: " + path);
                }
                int segmentEnd = findSegmentEnd(path, index);
                String content = path.substring(index + 1, segmentEnd);
                List<String> selectors = splitSelectors(content);
                List<String> nextPaths = new ArrayList<>(partialPaths.size() * selectors.size());
                for (String partialPath : partialPaths) {
                    for (String selector : selectors) {
                        nextPaths.add(partialPath + "[" + selector.strip() + "]");
                    }
                }
                partialPaths = nextPaths;
                index = segmentEnd + 1;
            }
            expanded.addAll(partialPaths);
        }
        return expanded;
    }

    private static int findSegmentEnd(String path, int segmentStart) {
        char quote = 0;
        boolean escaped = false;
        for (int index = segmentStart + 1; index < path.length(); index++) {
            char current = path.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == ']') {
                return index;
            }
        }
        throw new IllegalArgumentException("Unexpected normalized JSONPath: " + path);
    }

    private static List<String> splitSelectors(String content) {
        List<String> selectors = new ArrayList<>();
        char quote = 0;
        boolean escaped = false;
        int selectorStart = 0;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == ',') {
                selectors.add(content.substring(selectorStart, index));
                selectorStart = index + 1;
            }
        }
        selectors.add(content.substring(selectorStart));
        return selectors;
    }

    private static void setValue(Map<String, Object> document, String path, Object value) {
        if ("$".equals(path)) {
            if (!(value instanceof Map<?, ?> replacement)) {
                throw new IllegalArgumentException("The root of the target document must remain an object");
            }
            document.clear();
            document.putAll((Map<String, Object>) replacement);
        } else {
            JsonPath.parse(document).set(path, value);
        }
    }

    private static NodeCategory categoryOf(Object value) {
        if (value instanceof Map<?, ?>) {
            return NodeCategory.OBJECT;
        }
        if (value instanceof List<?>) {
            return NodeCategory.ARRAY;
        }
        return NodeCategory.PRIMITIVE;
    }

    private static IllegalArgumentException incompatibleTypes(int actionIndex, Object target, Object update) {
        return actionError(actionIndex,
                "cannot apply " + categoryOf(update).displayName + " value to "
                        + categoryOf(target).displayName + " target");
    }

    private static IllegalArgumentException incompatiblePropertyTypes(int actionIndex, String property,
                                                                       Object target, Object update) {
        return actionError(actionIndex,
                "incompatible values for property '" + property + "': "
                        + categoryOf(target).displayName + " and " + categoryOf(update).displayName);
    }

    private static void requireString(Map<?, ?> object, String field, String objectName) {
        if (!(object.get(field) instanceof String)) {
            throw new IllegalArgumentException(objectName + " field '" + field + "' is required and must be a string");
        }
    }

    private static IllegalArgumentException actionError(int index, String message) {
        return new IllegalArgumentException("Overlay action " + index + " " + message);
    }

    private static Map<String, Object> deepCopyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(Objects.toString(key), deepCopy(value)));
        return copy;
    }

    private static Object deepCopy(Object source) {
        if (source instanceof Map<?, ?> map) {
            return deepCopyMap(map);
        }
        if (source instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object value : list) {
                copy.add(deepCopy(value));
            }
            return copy;
        }
        return source;
    }

    private enum FeatureSet {
        V1_0,
        V1_1
    }

    private enum NodeCategory {
        OBJECT("object"),
        ARRAY("array"),
        PRIMITIVE("primitive");

        private final String displayName;

        NodeCategory(String displayName) {
            this.displayName = displayName;
        }
    }

    private record NodeReference(String path, String parentPath, Integer arrayIndex, int depth, Object value) {

        static NodeReference from(String path, Object value) {
            if ("$".equals(path)) {
                return new NodeReference(path, "", null, 0, value);
            }

            List<Integer> segmentStarts = segmentStarts(path);
            int lastSegmentStart = segmentStarts.get(segmentStarts.size() - 1);
            String parentPath = path.substring(0, lastSegmentStart);
            String lastSegment = path.substring(lastSegmentStart);
            Integer arrayIndex = parseArrayIndex(lastSegment);
            return new NodeReference(path, parentPath, arrayIndex, segmentStarts.size(), value);
        }

        NodeCategory category() {
            return categoryOf(value);
        }

        private static List<Integer> segmentStarts(String path) {
            if (path.isEmpty() || path.charAt(0) != '$') {
                throw new IllegalArgumentException("Unexpected normalized JSONPath: " + path);
            }

            List<Integer> starts = new ArrayList<>();
            int index = 1;
            while (index < path.length()) {
                if (path.charAt(index) != '[') {
                    throw new IllegalArgumentException("Unexpected normalized JSONPath: " + path);
                }
                starts.add(index);
                index++;
                if (index < path.length() && (path.charAt(index) == '\'' || path.charAt(index) == '"')) {
                    char quote = path.charAt(index++);
                    boolean escaped = false;
                    while (index < path.length()) {
                        char current = path.charAt(index++);
                        if (escaped) {
                            escaped = false;
                        } else if (current == '\\') {
                            escaped = true;
                        } else if (current == quote) {
                            break;
                        }
                    }
                } else {
                    while (index < path.length() && path.charAt(index) != ']') {
                        index++;
                    }
                }
                if (index >= path.length() || path.charAt(index) != ']') {
                    throw new IllegalArgumentException("Unexpected normalized JSONPath: " + path);
                }
                index++;
            }
            return starts;
        }

        private static Integer parseArrayIndex(String segment) {
            if (segment.length() < 3 || segment.charAt(1) == '\'' || segment.charAt(1) == '"') {
                return null;
            }
            try {
                return Integer.valueOf(segment.substring(1, segment.length() - 1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
