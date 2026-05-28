package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.AgentDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.AgentModelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.AgentToolFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.MessagePointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AgentFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<MessagePointerFrontmatter> sends,
        List<MessagePointerFrontmatter> receives,
        List<ResourcePointerFrontmatter> writesTo,
        List<ResourcePointerFrontmatter> readsFrom,
        List<ResourcePointerFrontmatter> flows,
        AgentModelFrontmatter model,
        List<AgentToolFrontmatter> tools,
        AgentDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
