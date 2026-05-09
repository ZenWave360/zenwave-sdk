package io.zenwave360.sdk.plugins.frontmatter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typed frontmatter for command {@code index.mdx} pages. */
public class CommandFrontmatter {

    public String id;
    public String name;
    public String version;
    public String schemaPath;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        if (schemaPath != null) map.put("schemaPath", schemaPath);
        return map;
    }
}
