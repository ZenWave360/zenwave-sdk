package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.CommonFrontmatter;
import io.zenwave360.sdk.plugins.frontmatter.FrontmatterTypes.ContainerDetailsPanelFrontmatter;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ContainerFrontmatter(
        @JsonUnwrapped CommonFrontmatter base,
        @JsonProperty("container_type") String containerType,
        String technology,
        Boolean authoritative,
        @JsonProperty("access_mode") String accessMode,
        String classification,
        String residency,
        String retention,
        ContainerDetailsPanelFrontmatter detailsPanel) implements Frontmatter {
}
