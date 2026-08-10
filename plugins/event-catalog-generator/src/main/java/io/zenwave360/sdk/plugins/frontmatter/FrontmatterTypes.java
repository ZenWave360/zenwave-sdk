package io.zenwave360.sdk.plugins.frontmatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class FrontmatterTypes {

    private FrontmatterTypes() {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CommonFrontmatter(
            String id,
            String name,
            String summary,
            String version,
            DraftFrontmatter draft,
            List<BadgeFrontmatter> badges,
            List<OwnerFrontmatter> owners,
            String schemaPath,
            List<SpecificationFrontmatter> specifications,
            SidebarFrontmatter sidebar,
            RepositoryFrontmatter repository,
            Boolean hidden,
            String editUrl,
            List<ResourceGroupFrontmatter> resourceGroups,
            StylesFrontmatter styles,
            DeprecatedFrontmatter deprecated,
            Boolean visualiser,
            List<AttachmentFrontmatter> attachments,
            List<ResourcePointerFrontmatter> diagrams,
            List<String> versions,
            String latestVersion) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DraftFrontmatter(String title, String message) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record BadgeFrontmatter(String content, String backgroundColor, String textColor, String icon, String url) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record OwnerFrontmatter(String id) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record SpecificationFrontmatter(String type, String path, String name, Map<String, String> headers) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record SidebarFrontmatter(String label, String badge, String color, String backgroundColor) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RepositoryFrontmatter(String language, String url) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ResourceGroupFrontmatter(String id, String title, List<ResourcePointerFrontmatter> items, Integer limit, Boolean sidebar) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record StylesFrontmatter(String icon, NodeStyleFrontmatter node) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record NodeStyleFrontmatter(String color, String label) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DeprecatedFrontmatter(String message, String date) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AttachmentFrontmatter(String url, String title, String type, String description, String icon) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ResourcePointerFrontmatter(String id, String version, String type) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ChannelPointerFrontmatter(String id, String version, Map<String, String> parameters) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ChannelRouteFrontmatter(
            String id,
            String version,
            Map<String, String> parameters,
            @JsonProperty("delivery_mode") String deliveryMode) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MessagePointerFrontmatter(
            String id,
            String version,
            List<String> fields,
            String group,
            List<ChannelRouteFrontmatter> to,
            List<ChannelRouteFrontmatter> from) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record OperationFrontmatter(String method, String path, List<String> statusCodes) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DetailPanelPropertyFrontmatter(Boolean visible) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DomainDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter parentDomains,
            DetailPanelPropertyFrontmatter subdomains,
            DetailPanelPropertyFrontmatter services,
            DetailPanelPropertyFrontmatter entities,
            DetailPanelPropertyFrontmatter messages,
            DetailPanelPropertyFrontmatter ubiquitousLanguage,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter attachments) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ServiceDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter domains,
            DetailPanelPropertyFrontmatter messages,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter specifications,
            DetailPanelPropertyFrontmatter entities,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter containers) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MessageDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter producers,
            DetailPanelPropertyFrontmatter consumers,
            DetailPanelPropertyFrontmatter channels,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter attachments) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record EntityDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter domains,
            DetailPanelPropertyFrontmatter services,
            DetailPanelPropertyFrontmatter messages,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter attachments) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ChannelDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter producers,
            DetailPanelPropertyFrontmatter consumers,
            DetailPanelPropertyFrontmatter messages,
            DetailPanelPropertyFrontmatter protocols,
            DetailPanelPropertyFrontmatter parameters,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter attachments) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter changelog) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AgentDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter domains,
            DetailPanelPropertyFrontmatter messages,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter tools,
            DetailPanelPropertyFrontmatter model) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DataProductDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter domains,
            DetailPanelPropertyFrontmatter inputs,
            DetailPanelPropertyFrontmatter outputs,
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter flows) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DiagramDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog,
            DetailPanelPropertyFrontmatter attachments) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ContainerDetailsPanelFrontmatter(
            DetailPanelPropertyFrontmatter versions,
            DetailPanelPropertyFrontmatter repository,
            DetailPanelPropertyFrontmatter owners,
            DetailPanelPropertyFrontmatter changelog) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record EntityPropertyFrontmatter(
            String name,
            String type,
            Boolean required,
            String description,
            String references,
            String referencesIdentifier,
            String relationType,
            @JsonProperty("enum") List<String> enumValues,
            EntityPropertyItemsFrontmatter items) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record EntityPropertyItemsFrontmatter(String type) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ChannelParameterFrontmatter(
            @JsonProperty("enum") List<String> enumValues,
            @JsonProperty("default") String defaultValue,
            List<String> examples,
            String description) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ChannelMessageFrontmatter(String collection, String name, String id, String version) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AgentToolFrontmatter(String name, String type, String icon, String url, String description) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AgentModelFrontmatter(String provider, String name, String version) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DataProductOutputFrontmatter(String id, String version, DataProductContractFrontmatter contract) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record DataProductContractFrontmatter(String path, String name, String type) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowStepFrontmatter(
            Object id,
            String type,
            String title,
            String summary,
            ResourcePointerFrontmatter message,
            ResourcePointerFrontmatter agent,
            ResourcePointerFrontmatter service,
            ResourcePointerFrontmatter flow,
            ResourcePointerFrontmatter container,
            ResourcePointerFrontmatter dataProduct,
            FlowActorFrontmatter actor,
            FlowCustomFrontmatter custom,
            FlowExternalSystemFrontmatter externalSystem,
            @JsonProperty("next_step") FlowNextStepFrontmatter nextStep,
            @JsonProperty("next_steps") List<FlowNextStepFrontmatter> nextSteps) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowActorFrontmatter(String name, String summary) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowCustomFrontmatter(
            String title,
            String icon,
            String type,
            String summary,
            String url,
            String color,
            Map<String, Object> properties,
            Integer height,
            List<FlowMenuFrontmatter> menu) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowMenuFrontmatter(String label, String url) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowExternalSystemFrontmatter(String name, String summary, String url) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FlowNextStepFrontmatter(Object id, String label) {
    }
}
