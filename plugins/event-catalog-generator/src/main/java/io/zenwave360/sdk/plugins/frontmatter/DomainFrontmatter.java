package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.DomainDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.MessagePointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DomainFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<ResourcePointerFrontmatter> services,
        List<ResourcePointerFrontmatter> agents,
        List<ResourcePointerFrontmatter> domains,
        @JsonProperty("data-products") List<ResourcePointerFrontmatter> dataProducts,
        List<ResourcePointerFrontmatter> entities,
        List<ResourcePointerFrontmatter> flows,
        List<MessagePointerFrontmatter> sends,
        List<MessagePointerFrontmatter> receives,
        DomainDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
