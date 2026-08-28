package io.zenwave360.sdk.zdl.annotators;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.layouts.CleanHexagonalProjectLayout;
import io.zenwave360.sdk.zdl.layouts.HexagonalProjectLayout;
import io.zenwave360.sdk.zdl.layouts.LayeredProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotation;
import io.zenwave360.sdk.zdl.utils.ZDLAnnotator;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import java.util.List;
import java.util.Map;

import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.*;

/**
 * Adds <a href="https://jmolecules.org">jMolecules</a> DDD and architecture annotations.
 *
 * <p>
 * Annotation based only: generated types are not made to implement {@code AggregateRoot<T, ID>} or
 * to use {@code Association<T, ID>}. The SDK needs no compile time dependency on jMolecules, since
 * annotations are emitted as fully qualified names; only the generated project needs the jMolecules
 * artifacts on its classpath.
 * </p>
 *
 * <p>
 * Note how a single {@link #annotate(JavaZdlModel.Entity, Map, Map)} call contributes to two
 * different artifacts: the entity POJO gets {@code @AggregateRoot}, while the repository generated
 * from the same entity gets {@code @Repository}.
 * </p>
 */
public class JMoleculesAnnotator implements ZDLAnnotator {

    public enum Architecture {
        NONE, HEXAGONAL, LAYERED
    }

    /**
     * The architecture vocabulary implied by a project layout. There is no option to override it:
     * a layout that needs a different vocabulary should be mapped here, or extend the layout whose
     * vocabulary it wants.
     *
     * <p>
     * Layouts that do not separate ports from the rest of the code map to {@link Architecture#NONE}:
     * {@code SimpleDomainProjectLayout} puts repositories in the base package itself, so calling one
     * a {@code @SecondaryPort} would claim a boundary that does not exist. {@code CleanArchitecture}
     * has a real boundary, but its vocabulary is onion, which is not implemented yet.
     * </p>
     */
    public static Architecture architectureOf(ProjectLayout layout) {
        if (layout instanceof LayeredProjectLayout) {
            return Architecture.LAYERED;
        }
        // DefaultProjectLayout extends CleanHexagonalProjectLayout
        if (layout instanceof HexagonalProjectLayout || layout instanceof CleanHexagonalProjectLayout) {
            return Architecture.HEXAGONAL;
        }
        return Architecture.NONE;
    }

    private static final String DDD = "org.jmolecules.ddd.annotation.";
    private static final String EVENT = "org.jmolecules.event.annotation.";
    private static final String HEX = "org.jmolecules.architecture.hexagonal.";
    private static final String LAYER = "org.jmolecules.architecture.layered.";

    private final Architecture architecture;

    public JMoleculesAnnotator() {
        this(Architecture.NONE);
    }

    public JMoleculesAnnotator(Architecture architecture) {
        this.architecture = architecture;
    }

    @Override
    public void annotate(JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
        if (isValueObject(zdlEntity)) {
            // @vo and @embedded are the model's own statement that this type has no identity, so it
            // is annotated like any other value object, the same way inbound DTOs are
            entity.addAnnotation(Annotation.on(DDD + "ValueObject", DOMAIN_ENTITY));
            return; // value objects have no identity and no repository
        }

        // @AggregateRoot is meta-annotated with @Entity: emit one or the other, never both
        var isAggregateRoot = isAggregateRoot(entity, zdlEntity, zdl);
        entity.addAnnotation(isAggregateRoot
                ? Annotation.on(DDD + "AggregateRoot", DOMAIN_ENTITY)
                : Annotation.on(DDD + "Entity", DOMAIN_ENTITY));

        if (architecture == Architecture.LAYERED) {
            entity.addAnnotation(Annotation.on(LAYER + "DomainLayer", DOMAIN_ENTITY));
        }

        if (!isAggregateRoot) {
            return; // only aggregate roots get a repository
        }

        // same model element, different generated artifact
        entity.addAnnotation(Annotation.on(DDD + "Repository", OUTBOUND_REPOSITORY_PORT));

        if (architecture == Architecture.HEXAGONAL) {
            // the repository interface lives in the outbound package, so it is the aggregate's
            // secondary port, even though it extends a Spring Data interface
            entity.addAnnotation(Annotation.on(HEX + "SecondaryPort", OUTBOUND_REPOSITORY_PORT));
        }
        if (architecture == Architecture.LAYERED) {
            // LayeredProjectLayout is the three tier web -> service -> repository, so the repository
            // is the persistence tier. This is deliberately *not* bent to satisfy
            // JMoleculesArchitectureRules.ensureLayering(), which encodes Evans' layering instead;
            // that rule is not generated for layered projects.
            entity.addAnnotation(Annotation.on(LAYER + "InfrastructureLayer", OUTBOUND_REPOSITORY_PORT));
        }
    }

    @Override
    public void annotate(JavaZdlModel.Field field, JavaZdlModel.Annotated owner, Map<String, Object> zdlOwner, Map<String, Object> zdl) {
        if (owner instanceof JavaZdlModel.Entity entity && field == entity.idField()) {
            field.addAnnotation(Annotation.on(DDD + "Identity", DOMAIN_ENTITY));
        }
    }

    @Override
    public void annotate(JavaZdlModel.Event event, Map<String, Object> zdlEvent, Map<String, Object> zdl) {
        if (JSONPath.get(zdlEvent, "$.options.asyncapi") != null) {
            // @asyncapi payload DTOs are generated from the AsyncAPI contract, they are not domain events
            return;
        }
        event.addAnnotation(Annotation.on(EVENT + "DomainEvent", DOMAIN_EVENT));
        if (architecture == Architecture.LAYERED) {
            event.addAnnotation(Annotation.on(LAYER + "DomainLayer", DOMAIN_EVENT));
        }
    }

    @Override
    public void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
        // deliberately no ddd @Service: that denotes a *domain* service, while ZenWave services are
        // application/use-case services
        switch (architecture) {
            case HEXAGONAL -> {
                service.addAnnotation(Annotation.on(HEX + "PrimaryPort", INBOUND_SERVICE_PORT));
                service.addAnnotation(Annotation.on(HEX + "Application", APPLICATION_SERVICE_IMPL));
            }
            case LAYERED -> service.addAnnotation(
                    Annotation.on(LAYER + "ApplicationLayer", INBOUND_SERVICE_PORT, APPLICATION_SERVICE_IMPL));
            case NONE -> {
            }
        }
    }

    @Override
    public void annotate(JavaZdlModel javaModel, Map<String, Object> zdl) {
        ZDLAnnotator.super.annotate(javaModel, zdl);

        // lets templates that generate architecture verification match the vocabulary we emitted
        javaModel.jmoleculesArchitecture = architecture.name();

        // ZDL inputs and outputs are identity free, attribute defined structures: value objects.
        // Contributed at artifact level because a single template renders both of them.
        javaModel.addArtifactAnnotation(INBOUND_DTO, Annotation.of(DDD + "ValueObject"));

        // the package-info of a Spring Modulith module is the one package ZenWave already treats as
        // a module boundary, so it is where @Module belongs
        javaModel.addArtifactAnnotation(PACKAGE_INFO_MODULE, Annotation.of(DDD + "Module"));

        // artifacts with no backing ZDL element
        if (architecture == Architecture.HEXAGONAL) {
            javaModel.addArtifactAnnotation(OUTBOUND_EVENT_PUBLISHER_PORT, Annotation.of(HEX + "SecondaryPort"));
            javaModel.addArtifactAnnotation(INFRASTRUCTURE_EVENT_PUBLISHER, Annotation.of(HEX + "SecondaryAdapter"));
            javaModel.addArtifactAnnotation(ADAPTER_EVENT_LISTENER, Annotation.of(HEX + "PrimaryAdapter"));
            javaModel.addArtifactAnnotation(ADAPTER_WEB_CONTROLLER, Annotation.of(HEX + "PrimaryAdapter"));
            javaModel.addArtifactAnnotation(ADAPTER_ASYNCAPI_CONSUMER, Annotation.of(HEX + "PrimaryAdapter"));
        }
        if (architecture == Architecture.LAYERED) {
            javaModel.addArtifactAnnotation(INFRASTRUCTURE_EVENT_PUBLISHER, Annotation.of(LAYER + "InfrastructureLayer"));
            javaModel.addArtifactAnnotation(ADAPTER_WEB_CONTROLLER, Annotation.of(LAYER + "InterfaceLayer"));
            javaModel.addArtifactAnnotation(ADAPTER_EVENT_LISTENER, Annotation.of(LAYER + "InterfaceLayer"));
            javaModel.addArtifactAnnotation(ADAPTER_ASYNCAPI_CONSUMER, Annotation.of(LAYER + "InterfaceLayer"));
        }
    }

    protected boolean isValueObject(Map<String, Object> zdlEntity) {
        return !JSONPath.get(zdlEntity, "$.options[?(@.vo || @.embedded)]", List.of()).isEmpty();
    }

    /**
     * Mirrors the rule the templates use to decide whether an entity gets a repository: an entity
     * annotated {@code @aggregate} or {@code @lifecycle}, or the root of a declared
     * {@code aggregate Foo(Bar)}. {@code ZDLFindUtils.isAggregateRoot} alone only covers the last of
     * the three.
     */
    protected boolean isAggregateRoot(JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
        var hasAggregateOption = !JSONPath.get(zdlEntity, "$.options[?(@.aggregate || @.lifecycle)]", List.of()).isEmpty();
        return hasAggregateOption || ZDLFindUtils.isAggregateRoot(zdl, entity.name());
    }
}
