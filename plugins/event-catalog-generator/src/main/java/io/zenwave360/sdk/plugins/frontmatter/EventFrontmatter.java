package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ChannelPointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.MessageDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.OperationFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record EventFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        OperationFrontmatter operation,
        List<ResourcePointerFrontmatter> producers,
        List<ResourcePointerFrontmatter> consumers,
        List<ChannelPointerFrontmatter> channels,
        List<ResourcePointerFrontmatter> messageChannels,
        MessageDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
