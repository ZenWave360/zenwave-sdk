package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.EntityDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.EntityPropertyFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record EntityFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        Boolean aggregateRoot,
        String identifier,
        List<EntityPropertyFrontmatter> properties,
        List<ResourcePointerFrontmatter> services,
        List<ResourcePointerFrontmatter> domains,
        EntityDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
