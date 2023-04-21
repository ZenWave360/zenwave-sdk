package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.utils.JSONPath;

public class JDLBackendApplicationDefaultJpaHelpers {

    private final JDLBackendApplicationDefaultGenerator generator;

    JDLBackendApplicationDefaultJpaHelpers(JDLBackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public Boolean addRelationshipById(Object relationship, Options options) {
        var jdl = options.get("jdl");
        String otherEntityName = JSONPath.get(relationship, "otherEntityName");
        boolean isOtherEntityAggregate = JSONPath.get(jdl, String.format("entities.%s.options.aggregate", otherEntityName), false);
        boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
        String relationType = JSONPath.get(relationship, "type");
        return "ManyToOne".contentEquals(relationType) && isOwnerSide && isOtherEntityAggregate;
    }
}
