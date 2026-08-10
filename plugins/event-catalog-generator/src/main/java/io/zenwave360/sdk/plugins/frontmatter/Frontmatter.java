package io.zenwave360.sdk.plugins.frontmatter;

import java.util.Map;

public interface Frontmatter {

    default Map<String, Object> toMap() {
        return FrontmatterMapper.toMap(this);
    }
}
