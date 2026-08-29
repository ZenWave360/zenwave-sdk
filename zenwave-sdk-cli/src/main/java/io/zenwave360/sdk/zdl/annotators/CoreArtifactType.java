package io.zenwave360.sdk.zdl.annotators;

import java.util.Arrays;
import java.util.Optional;

/**
 * Well known generated artifact types shipped with the SDK.
 *
 * <p>
 * Several templates may legitimately share one value: the jpa, mongodb and vo variants of
 * {@code Entity.java.hbs} all declare {@link #DOMAIN_ENTITY}. This is a classification, not an
 * identity.
 * </p>
 *
 * <p>
 * Test templates deliberately declare no artifact type and never call {@code {{annotate}}}, so
 * annotations cannot leak into generated test sources.
 * </p>
 */
public enum CoreArtifactType implements ArtifactType {

    // ── Domain ────────────────────────────────────────────────────────────────
    /** core/domain/{jpa,mongodb,vo}/Entity.java — entity POJO (root, entity or value object) */
    DOMAIN_ENTITY("domain.entity"),
    /** core/domain/common/Aggregate.java — aggregate wrapper, not the DDD root itself */
    DOMAIN_AGGREGATE("domain.aggregate"),
    /** core/domain/common/AggregateTransitions.java */
    DOMAIN_AGGREGATE_TRANSITIONS("domain.aggregate-transitions"),
    /** core/domain/common/EntityTransitions.java */
    DOMAIN_ENTITY_TRANSITIONS("domain.entity-transitions"),
    /** core/domain/common/DomainEvent.java */
    DOMAIN_EVENT("domain.event"),
    /** core/domain/common/DomainEnum.java */
    DOMAIN_ENUM("domain.enum"),

    // ── Inbound / primary ports ───────────────────────────────────────────────
    /** core/inbound/Service.java — inbound service port */
    INBOUND_SERVICE_PORT("inbound.service-port"),
    /** core/inbound/dtos/InputOrOutput.java — inbound DTO, shared by inputs and outputs */
    INBOUND_DTO("inbound.dto"),
    /** core/domain/common/InputEnum.java */
    INBOUND_DTO_ENUM("inbound.dto-enum"),
    /** core/domain/common/EventEnum.java */
    INBOUND_EVENT_ENUM("inbound.event-enum"),

    // ── Application / core implementation ─────────────────────────────────────
    /** core/implementation/{style}/ServiceImpl.java */
    APPLICATION_SERVICE_IMPL("application.service-impl"),
    /** core/implementation/mappers/ServiceMapper.java */
    APPLICATION_SERVICE_MAPPER("application.service-mapper"),
    /** core/implementation/mappers/EventsMapper.java */
    APPLICATION_EVENTS_MAPPER("application.events-mapper"),
    /** core/implementation/mappers/BaseMapper.java */
    APPLICATION_BASE_MAPPER("application.base-mapper"),

    // ── Outbound / secondary ports ────────────────────────────────────────────
    /** core/outbound/{persistence}/{style}/EntityRepository.java */
    OUTBOUND_REPOSITORY_PORT("outbound.repository-port"),
    /** core/outbound/events/EventPublisher.java */
    OUTBOUND_EVENT_PUBLISHER_PORT("outbound.event-publisher-port"),

    // ── Infrastructure / secondary adapters ───────────────────────────────────
    /** infrastructure/events/DefaultEventPublisher.java */
    INFRASTRUCTURE_EVENT_PUBLISHER("infrastructure.event-publisher"),
    /** infrastructure repository implementations, where a layout generates them */
    INFRASTRUCTURE_REPOSITORY("infrastructure.repository"),

    // ── Primary adapters ──────────────────────────────────────────────────────
    /** web/{webFlavor}/ServiceApiController.java — openapi-controllers plugin */
    ADAPTER_WEB_CONTROLLER("adapter.web-controller"),
    /** web/mappers/*.java — openapi-controllers plugin */
    ADAPTER_WEB_MAPPER("adapter.web-mapper"),
    /** adapters/events/EventListeners.java */
    ADAPTER_EVENT_LISTENER("adapter.event-listener"),
    /** adapters/events/EventListenersMapper.java, EventListenersMapStructMapper.java */
    ADAPTER_EVENT_LISTENER_MAPPER("adapter.event-listener-mapper"),
    /** adapters/events/asyncapi/{style}/ConsumerService.java */
    ADAPTER_ASYNCAPI_CONSUMER("adapter.asyncapi-consumer"),
    /** adapters/events/asyncapi/EventsMapper.java, EventsMapStructMapper.java */
    ADAPTER_ASYNCAPI_MAPPER("adapter.asyncapi-mapper"),

    // ── package-info ──────────────────────────────────────────────────────────
    /** package-info.java at the module root (Spring Modulith) */
    PACKAGE_INFO_MODULE("package-info.module"),
    /** common-package-info.java */
    PACKAGE_INFO_COMMON("package-info.common"),
    /** core/inbound/dtos/package-info.java */
    PACKAGE_INFO_INBOUND_DTOS("package-info.inbound-dtos"),
    /** infrastructure/package-info.java */
    PACKAGE_INFO_INFRASTRUCTURE("package-info.infrastructure");

    private final String id;

    CoreArtifactType(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    public static Optional<CoreArtifactType> of(String id) {
        return Arrays.stream(values()).filter(artifactType -> artifactType.id.equals(id)).findFirst();
    }
}
