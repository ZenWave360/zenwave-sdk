package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.FlowStepFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FlowFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<FlowStepFrontmatter> steps,
        FlowDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
