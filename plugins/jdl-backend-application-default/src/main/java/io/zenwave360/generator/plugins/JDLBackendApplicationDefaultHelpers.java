package io.zenwave360.generator.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.Options;

import io.zenwave360.generator.options.PersistenceType;
import io.zenwave360.generator.utils.JSONPath;
import org.apache.commons.io.FilenameUtils;

public class JDLBackendApplicationDefaultHelpers {

    private final JDLBackendApplicationDefaultGenerator generator;

    JDLBackendApplicationDefaultHelpers(JDLBackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public String fieldType(Object context, Options options) {
        Map field = (Map) context;
        String type = (String) field.get("type");
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        if (field.get("isArray") == Boolean.TRUE) {
            return String.format("List<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public String relationshipFieldType(Object context, Options options) {
        Map field = (Map) context;
        String type = (String) field.get("otherEntityName");
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        if (field.get("isCollection") == Boolean.TRUE) {
            return String.format("Set<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public String fieldPersistenceAnnotations(Object context, Options options) {
        Map field = (Map) context;
        if (generator.persistence == PersistenceType.mongodb) {
            // filtering with lowerFirst and upperFirst for forward jdl compatibility
            int dbRef = ((List) JSONPath.get(field, "options[?(@.dBRef || @.DBRef)]")).size();
            int documentedOptions = ((List) JSONPath.get(field, "options[?(@.documentReference || @.DocumentReference)]")).size();
            if (dbRef > 0) {
                return "@DBRef";
            }
            if (documentedOptions > 0) {
                return "@DocumentReference";
            }
            return "@Field";
        }
        return "";
    };

    public String fieldValidationAnnotations(Object context, Options options) {

        Map field = (Map) context;
        var required = JSONPath.get(field, "validations.required.value");
        var min = JSONPath.get(field, "validations.min.value");
        var max = JSONPath.get(field, "validations.max.value");
        var minlength = JSONPath.get(field, "validations.minlength.value");
        var maxlength = JSONPath.get(field, "validations.maxlength.value");
        var pattern = JSONPath.get(field, "validations.pattern.value");
        var unique = JSONPath.get(field, "validations.unique.value");
        List<String> annotations = new ArrayList<>();
        if (required != null) {
            annotations.add("@NotNull");
        }
        if (min != null) {
            annotations.add(String.format("@Min(%s)", min));
        }
        if (max != null) {
            annotations.add(String.format("@Max(%s)", max));
        }
        if (minlength != null || maxlength != null) {
            annotations.add(String.format("@Size(min = %s, max = %s)", minlength, maxlength));
        } else if (maxlength != null) {
            annotations.add(String.format("@Size(max = %s)", maxlength));
        } else if (minlength != null) {
            annotations.add(String.format("@Size(min = %s)", minlength));
        }
        if (pattern != null) {
            annotations.add(String.format("@Pattern(regexp = \"%s\")", pattern));
        }

        return annotations.stream().collect(Collectors.joining(" "));
    };

    public String criteriaClassName(Object context, Options options) {
        Map entity = (Map) context;
        Object criteria = JSONPath.get(entity, "$.options.searchCriteria");
        if (criteria instanceof String) {
            return (String) criteria;
        }
        if (criteria == Boolean.TRUE) {
            return String.format("%s%s", entity.get("className"), generator.criteriaDTOSuffix);
        }
        return "Pageable";
    };

    public Object skipEntityRepository(Object context, Options options) {
        Map entity = (Map) context;
        return generator.skipEntityRepository.apply(Map.of("entity", entity));
    };

    public Object skipEntityId(Object context, Options options) {
        Map entity = (Map) context;
        return generator.skipEntityId.apply(Map.of("entity", entity));
    };

}
