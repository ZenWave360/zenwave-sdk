package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.MessagePointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ServiceDetailsPanelFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ServiceFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<MessagePointerFrontmatter> sends,
        List<MessagePointerFrontmatter> receives,
        List<ResourcePointerFrontmatter> entities,
        List<ResourcePointerFrontmatter> writesTo,
        List<ResourcePointerFrontmatter> readsFrom,
        List<ResourcePointerFrontmatter> flows,
        Boolean externalSystem,
        ServiceDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
