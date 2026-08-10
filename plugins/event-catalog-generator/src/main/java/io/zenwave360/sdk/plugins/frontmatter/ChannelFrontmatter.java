package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ChannelDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ChannelMessageFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ChannelParameterFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ChannelPointerFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ChannelFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<ChannelPointerFrontmatter> channels,
        String address,
        List<String> protocols,
        String deliveryGuarantee,
        List<ChannelPointerFrontmatter> routes,
        Map<String, ChannelParameterFrontmatter> parameters,
        List<ChannelMessageFrontmatter> messages,
        ChannelDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
