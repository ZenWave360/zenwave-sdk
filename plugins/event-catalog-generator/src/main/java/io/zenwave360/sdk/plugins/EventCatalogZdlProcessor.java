package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.ZenWaveManifestLoader;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Parses ZDL artifacts declared in each service entry and augments the service map
 * with extracted entities and aggregates.
 */
public class EventCatalogZdlProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;
    @DocumentedOption(description = "Comma separated local roots for workspace-first content loading.")
    public String localRoots;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        ZenWaveManifestLoader manifestLoader = (ZenWaveManifestLoader) contextModel.get("manifestLoader");
        File manifestFile = (File) contextModel.get("manifestFile");
        if (architecture == null || manifest == null || manifestLoader == null) return contextModel;

        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());
        ManifestLoadOptions contentOptions = ManifestRuntimeSupport.contentOptions(
                manifest, manifestFile, preferredSource, allowFallback, localRoots);

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> serviceMap = (Map<String, Object>) entry.getValue();
            ManifestService service = ManifestRuntimeSupport.findService(manifest, serviceMap);
            if (service == null) {
                continue;
            }
            String serviceId = str(serviceMap, "id", entry.getKey());
            processZdlArtifacts(manifestLoader, manifest, service, serviceMap, serviceId, contentOptions);
        }

        return contextModel;
    }

    private void processZdlArtifacts(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest,
                                     ManifestService manifestService, Map<String, Object> serviceMap,
                                     String serviceId, ManifestLoadOptions contentOptions) {
        for (ManifestArtifact artifact : ManifestRuntimeSupport.findArtifacts(manifestService, "zdl")) {
            String zdlText;
            try {
                zdlText = ManifestRuntimeSupport.loadArtifactText(manifestLoader, manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("ZDL artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> zdlModel = parseSpec(artifact, zdlText);
            if (zdlModel == null) continue;

            String version = str(serviceMap, "_version", str(serviceMap, "version", "0.0.1"));

            Map<String, Object> aggregates = JSONPath.get(zdlModel, "$.aggregates", Map.of());
            Set<String> aggregateRootNames = new LinkedHashSet<>();
            for (Map.Entry<String, Object> aggEntry : aggregates.entrySet()) {
                Map<String, Object> agg = (Map<String, Object>) aggEntry.getValue();
                String rootName = str(agg, "aggregateRoot", null);
                if (rootName != null) aggregateRootNames.add(rootName);
            }

            Map<String, Object> entities = JSONPath.get(zdlModel, "$.entities", Map.of());
            for (Map.Entry<String, Object> entityEntry : entities.entrySet()) {
                Map<String, Object> entity = (Map<String, Object>) entityEntry.getValue();
                String entityName = str(entity, "name", entityEntry.getKey());

                boolean isAggregate = aggregateRootNames.contains(entityName)
                        || Boolean.TRUE.equals(JSONPath.get(entity, "$.options.aggregate"));

                String entityId = serviceId + "." + toKebabCase(entityName);

                Map<String, Object> entityArtifact = new LinkedHashMap<>();
                entityArtifact.put("id", entityId);
                entityArtifact.put("name", entityName);
                entityArtifact.put("version", version);
                entityArtifact.put("summary", str(entity, "javadoc", ""));
                if (isAggregate) entityArtifact.put("aggregateRoot", true);
                String identifier = resolveIdentifier(entity);
                if (identifier != null) entityArtifact.put("identifier", identifier);
                List<Map<String, Object>> properties = buildProperties(entity, zdlModel);
                if (!properties.isEmpty()) entityArtifact.put("properties", properties);

                addToList(serviceMap, "_entities", entityArtifact);
            }
        }
    }

    private Map<String, Object> parseSpec(ManifestArtifact artifact, String zdlText) {
        File tempFile = null;
        String tempKey = "_ec_zdl_" + System.nanoTime();
        try {
            tempFile = File.createTempFile("event-catalog-zdl-", ".zdl");
            Files.writeString(tempFile.toPath(), zdlText);
            var parsed = new ZDLParser()
                    .withZdlFile(tempFile.getAbsolutePath())
                    .withTargetProperty(tempKey)
                    .parse();

            return (Map<String, Object>) parsed.get(tempKey);
        } catch (Exception e) {
            log.warn("Failed to parse ZDL artifact {}: {} ({})", artifact.getPathExpression(), e.getMessage(), e.getClass().getSimpleName());
            return null;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private String toKebabCase(String name) {
        if (name == null || name.isBlank()) return name;
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private String resolveIdentifier(Map<String, Object> entity) {
        List<Map> naturalIdFields = ZDLFindUtils.naturalIdFields(entity);
        if (naturalIdFields != null && !naturalIdFields.isEmpty()) {
            return str(naturalIdFields.get(0), "name", null);
        }

        List<Map<String, Object>> fields = JSONPath.get(entity, "$.fields[*]", List.of());
        return fields.stream()
                .map(field -> str(field, "name", null))
                .filter(Objects::nonNull)
                .filter(name -> "id".equalsIgnoreCase(name) || name.endsWith("Id"))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildProperties(Map<String, Object> entity, Map<String, Object> zdlModel) {
        List<Map<String, Object>> fields = JSONPath.get(entity, "$.fields[*]", List.of());
        if (fields.isEmpty()) {
            return List.of();
        }

        Map<String, Object> allEntitiesAndEnums = JSONPath.get(zdlModel, "$.allEntitiesAndEnums", Map.of());
        List<Map<String, Object>> properties = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String fieldName = str(field, "name", null);
            String fieldType = str(field, "type", null);
            if (fieldName == null || fieldType == null) {
                continue;
            }

            boolean isArray = Boolean.TRUE.equals(field.get("typeIsArray")) || fieldType.endsWith("[]");
            String normalizedType = isArray && fieldType.endsWith("[]")
                    ? fieldType.substring(0, fieldType.length() - 2)
                    : fieldType;

            Map<String, Object> property = new LinkedHashMap<>();
            property.put("name", fieldName);
            property.put("type", isArray ? "array" : normalizedType);

            if (hasRequiredValidation(field)) {
                property.put("required", true);
            }

            String description = str(field, "javadoc", str(field, "comment", null));
            if (description != null && !description.isBlank()) {
                property.put("description", description);
            }

            if (isArray) {
                property.put("items", Map.of("type", normalizedType));
            }

            Map<String, Object> relationship = findRelationship(entity, fieldName, zdlModel);
            if (relationship != null) {
                property.put("references", relationship.get("target"));
                property.put("referencesIdentifier", relationship.get("targetField"));
                property.put("relationType", relationship.get("relationType"));
            } else if (allEntitiesAndEnums.containsKey(normalizedType)) {
                property.put("references", normalizedType);
            }

            List<String> enumValues = resolveEnumValues(normalizedType, allEntitiesAndEnums);
            if (!enumValues.isEmpty()) {
                property.put("enum", enumValues);
            }

            properties.add(property);
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private boolean hasRequiredValidation(Map<String, Object> field) {
        Object validations = field.get("validations");
        if (validations instanceof Map<?, ?> validationMap) {
            return validationMap.containsKey("required");
        }
        if (validations instanceof Collection<?> validationList) {
            return validationList.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .anyMatch(validation -> "required".equals(str(validation, "name", null)));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findRelationship(Map<String, Object> entity, String fieldName, Map<String, Object> zdlModel) {
        String entityName = str(entity, "name", null);
        if (entityName == null) {
            return null;
        }

        Map<String, Object> relationships = JSONPath.get(zdlModel, "$.relationships", Map.of());
        for (Object entry : relationships.values()) {
            if (!(entry instanceof Map<?, ?> relationshipMap)) {
                continue;
            }
            String from = str((Map<String, Object>) relationshipMap, "from", null);
            String fromField = str((Map<String, Object>) relationshipMap, "injectedFieldInFrom", null);
            if (entityName.equals(from) && fieldName.equals(fromField)) {
                Map<String, Object> relationship = new LinkedHashMap<>();
                relationship.put("target", str((Map<String, Object>) relationshipMap, "to", null));
                relationship.put("targetField", str((Map<String, Object>) relationshipMap, "injectedFieldInTo", null));
                relationship.put("relationType", relationshipType(str((Map<String, Object>) relationshipMap, "type", null)));
                return relationship;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveEnumValues(String type, Map<String, Object> allEntitiesAndEnums) {
        Object enumObject = allEntitiesAndEnums.get(type);
        if (!(enumObject instanceof Map<?, ?> enumMap)) {
            return List.of();
        }
        if (!"enums".equals(str((Map<String, Object>) enumMap, "type", null))) {
            return List.of();
        }

        Map<String, Object> values = (Map<String, Object>) ((Map<String, Object>) enumMap).get("values");
        if (values == null) {
            return List.of();
        }
        return values.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(value -> str((Map<String, Object>) value, "name", null))
                .filter(Objects::nonNull)
                .toList();
    }

    private String relationshipType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "one-to-one" -> "oneToOne";
            case "one-to-many" -> "oneToMany";
            case "many-to-one" -> "manyToOne";
            case "many-to-many" -> "manyToMany";
            default -> type;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Map<String, Object> map, String key, T value) {
        List<T> list = (List<T>) map.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
    }

    private String str(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
