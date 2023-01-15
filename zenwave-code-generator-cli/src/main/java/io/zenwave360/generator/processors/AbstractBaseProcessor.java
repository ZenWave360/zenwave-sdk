package io.zenwave360.generator.processors;

import java.util.List;
import java.util.Map;

import io.zenwave360.generator.utils.NamingUtils;

public abstract class AbstractBaseProcessor implements Processor {

    public String targetProperty = "api";

    protected void addNormalizedTagName(Map<String, Object> operation) {
        if (operation != null) {
            String normalizedTagName = null;
            List tags = (List) operation.get("tags");
            if (tags != null) {
                String tag = (String) (tags.get(0) instanceof Map ? (String) ((Map) tags.get(0)).get("name") : tags.get(0));
                normalizedTagName = normalizeTagName(tag);
            }
            operation.put("x--normalizedTagName", normalizedTagName);
        }
    }

    protected String normalizeTagName(String tagName) {
        return NamingUtils.asJavaTypeName(tagName);
    }

    protected void addOperationIdVariants(Map<String, Object> operation) {
        if (operation != null) {
            operation.put("x--operationIdCamelCase", NamingUtils.camelCase((String) operation.get("operationId")));
            operation.put("x--operationIdKebabCase", NamingUtils.kebabCase((String) operation.get("operationId")));
        }
    }
}
