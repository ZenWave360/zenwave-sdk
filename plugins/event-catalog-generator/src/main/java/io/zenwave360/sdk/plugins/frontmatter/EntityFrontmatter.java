package io.zenwave360.sdk.plugins.frontmatter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typed frontmatter for entity {@code index.mdx} pages. */
public class EntityFrontmatter {

    public String id;
    public String name;
    public String version;
    public String summary;
    public Boolean aggregateRoot;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        if (summary != null && !summary.isBlank()) map.put("summary", summary);
        if (Boolean.TRUE.equals(aggregateRoot)) map.put("aggregateRoot", true);
        return map;
    }
}
