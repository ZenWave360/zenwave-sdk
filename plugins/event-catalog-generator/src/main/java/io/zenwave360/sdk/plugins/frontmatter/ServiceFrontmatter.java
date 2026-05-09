package io.zenwave360.sdk.plugins.frontmatter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Typed frontmatter for service {@code index.mdx} pages. */
public class ServiceFrontmatter {

    public String id;
    public String name;
    public String version;
    public List<String> sends;
    public List<String> receives;
    public List<String> specifications;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        if (sends != null && !sends.isEmpty()) map.put("sends", sends);
        if (receives != null && !receives.isEmpty()) map.put("receives", receives);
        if (specifications != null && !specifications.isEmpty()) map.put("specifications", specifications);
        return map;
    }
}
