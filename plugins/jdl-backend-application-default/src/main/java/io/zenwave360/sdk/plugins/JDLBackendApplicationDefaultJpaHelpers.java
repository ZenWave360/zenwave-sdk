package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JDLBackendApplicationDefaultJpaHelpers {

    private final JDLBackendApplicationDefaultGenerator generator;

    JDLBackendApplicationDefaultJpaHelpers(JDLBackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public Boolean addRelationshipById(Object relationship, Options options) {
        var jdl = options.get("jdl");
        String entityName = JSONPath.get(relationship, "entityName");
        String otherEntityName = JSONPath.get(relationship, "otherEntityName");
        boolean isAggregate = JSONPath.get(jdl, String.format("entities.%s.options.aggregate", entityName), false);
        boolean isOtherEntityAggregate = JSONPath.get(jdl, String.format("entities.%s.options.aggregate", otherEntityName), false);
        boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
        String relationType = JSONPath.get(relationship, "type");
        return ("OneToOne".contentEquals(relationType) || "ManyToOne".contentEquals(relationType)) && isOwnerSide && isAggregate && isOtherEntityAggregate;
    }

    public List<Map> findOwnedOneToManyRelationships(Map entity, Options options) {
        var relationships = JSONPath.get(entity, "relationships", Collections.<Map>emptyList());
        return relationships.stream()
            .filter(relationship -> {
                String relationType = JSONPath.get(relationship, "type");
                boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
                String otherEntityFieldName = JSONPath.get(relationship, "otherEntityFieldName");
                return relationType.endsWith("OneToMany") && isOwnerSide && StringUtils.isNotEmpty(otherEntityFieldName);
            })
            .collect(Collectors.toList());
    }
}
