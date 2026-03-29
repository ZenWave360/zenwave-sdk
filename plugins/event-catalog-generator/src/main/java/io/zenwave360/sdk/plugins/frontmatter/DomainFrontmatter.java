package io.zenwave360.sdk.plugins.frontmatter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typed frontmatter for domain and subdomain {@code index.mdx} pages. */
public class DomainFrontmatter {

    public String id;
    public String name;
    public String version;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        return map;
    }
}
