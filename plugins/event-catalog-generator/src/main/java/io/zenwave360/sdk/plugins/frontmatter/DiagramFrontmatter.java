package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.DiagramDetailsPanelFrontmatter;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DiagramFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        DiagramDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
