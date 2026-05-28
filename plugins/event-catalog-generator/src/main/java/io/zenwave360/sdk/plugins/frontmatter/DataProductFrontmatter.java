package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.DataProductDetailsPanelFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.DataProductOutputFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ResourcePointerFrontmatter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DataProductFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        List<ResourcePointerFrontmatter> inputs,
        List<DataProductOutputFrontmatter> outputs,
        DataProductDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
