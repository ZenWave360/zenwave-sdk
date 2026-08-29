package io.zenwave360.sdk.templating;

import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotated;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.DOMAIN_ENTITY;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.INBOUND_DTO;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.OUTBOUND_REPOSITORY_PORT;

/**
 * The {@code {{annotate}}} helper, exercised through the real engine the generators use, so the
 * helper registration in {@link HandlebarsEngine} is part of what is under test.
 */
class AnnotationHelperTest {

    record Element(List<Annotation> annotations) implements Annotated {
        Element() {
            this(new ArrayList<>());
        }
    }

    private final HandlebarsEngine engine = new HandlebarsEngine();

    private String render(String template, Map<String, Object> model) throws IOException {
        return engine.processInline(template, model);
    }

    // ── element annotations ───────────────────────────────────────────────────

    @Test
    void rendersOnlyTheAnnotationsScopedToTheArtifactBeingGenerated() throws IOException {
        // the helper's reason to exist: one model element, different annotations per generated file
        var entity = new Element();
        entity.addAnnotation(Annotation.on("org.jmolecules.ddd.annotation.AggregateRoot", DOMAIN_ENTITY));
        entity.addAnnotation(Annotation.on("org.jmolecules.ddd.annotation.Repository", OUTBOUND_REPOSITORY_PORT));

        var model = Map.<String, Object>of("entity", entity);
        Assertions.assertEquals("@org.jmolecules.ddd.annotation.AggregateRoot",
                render("{{annotate \"domain.entity\" entity}}", model));
        Assertions.assertEquals("@org.jmolecules.ddd.annotation.Repository",
                render("{{annotate \"outbound.repository-port\" entity}}", model));
        Assertions.assertEquals("", render("{{annotate \"inbound.dto\" entity}}", model));
    }

    @Test
    void rendersOneAnnotationPerLine() throws IOException {
        var entity = new Element();
        entity.addAnnotation(Annotation.of("com.acme.First"));
        entity.addAnnotation(Annotation.of("com.acme.Second"));

        Assertions.assertEquals("@com.acme.First\n@com.acme.Second",
                render("{{annotate \"domain.entity\" entity}}", Map.of("entity", entity)));
    }

    @Test
    void deduplicatesAnnotationsThatRenderIdentically() throws IOException {
        // two differently scoped annotations of the same type both match this artifact
        var entity = new Element();
        entity.addAnnotation(Annotation.on("com.acme.Marker", DOMAIN_ENTITY));
        entity.addAnnotation(Annotation.on("com.acme.Marker", DOMAIN_ENTITY, INBOUND_DTO));

        Assertions.assertEquals("@com.acme.Marker",
                render("{{annotate \"domain.entity\" entity}}", Map.of("entity", entity)));
    }

    @Test
    void rendersAnnotationValueWhenPresent() throws IOException {
        var entity = new Element();
        entity.addAnnotation(new Annotation("com.acme.Marker", "name = 'x'", null));
        entity.addAnnotation(Annotation.of("com.acme.Plain"));

        // triple stache: assert what the helper produces, not Handlebars' HTML escaping of it
        Assertions.assertEquals("@com.acme.Marker(name = 'x')\n@com.acme.Plain",
                render("{{{annotate \"domain.entity\" entity}}}", Map.of("entity", entity)));
    }

    // ── absent and non-annotated elements ─────────────────────────────────────

    @Test
    void absentModelNodeRendersNothingRatherThanArtifactAnnotations() throws IOException {
        // {{annotate "domain.entity" entity.javaEntity.idField}} on a value object: idField is null.
        // It must render nothing, never fall through to the artifact level annotations.
        var javaModel = new JavaZdlModel(Map.of());
        javaModel.addArtifactAnnotation(DOMAIN_ENTITY, Annotation.of("com.acme.ArtifactLevel"));

        var model = Map.<String, Object>of("zdl", Map.of("javaModel", javaModel));
        Assertions.assertEquals("", render("{{annotate \"domain.entity\" missing}}", model));
    }

    @Test
    void nonAnnotatedArgumentRendersNothing() throws IOException {
        var model = Map.<String, Object>of("notAnElement", "a plain string");
        Assertions.assertEquals("", render("{{annotate \"domain.entity\" notAnElement}}", model));
    }

    // ── artifact level annotations ────────────────────────────────────────────

    @Test
    void elementlessCallRendersArtifactLevelAnnotations() throws IOException {
        // for generated files with no backing ZDL element, such as EventPublisher or package-info
        var javaModel = new JavaZdlModel(Map.of());
        javaModel.addArtifactAnnotation(INBOUND_DTO, Annotation.of("org.jmolecules.ddd.annotation.ValueObject"));

        var model = Map.<String, Object>of("zdl", Map.of("javaModel", javaModel));
        Assertions.assertEquals("@org.jmolecules.ddd.annotation.ValueObject",
                render("{{annotate \"inbound.dto\"}}", model));
        Assertions.assertEquals("", render("{{annotate \"domain.entity\"}}", model));
    }

    @Test
    void elementlessCallRendersNothingWithoutAZdlModel() throws IOException {
        // templates are rendered for non-ZDL plugins too
        Assertions.assertEquals("", render("{{annotate \"inbound.dto\"}}", new HashMap<>()));
    }

    @Test
    void elementlessCallRendersNothingWhenTheZdlHasNoJavaModel() throws IOException {
        // annotators never ran: the zdl model is there but carries no javaModel
        var model = Map.<String, Object>of("zdl", new HashMap<String, Object>());
        Assertions.assertEquals("", render("{{annotate \"inbound.dto\"}}", model));
    }
}
