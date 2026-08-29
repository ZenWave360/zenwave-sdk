package io.zenwave360.sdk.zdl.model;

import io.zenwave360.sdk.zdl.annotators.ArtifactType;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotated;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.DOMAIN_ENTITY;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.INBOUND_DTO;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.OUTBOUND_REPOSITORY_PORT;

/**
 * The annotation model itself: artifact scoping and the idempotence contract annotators rely on to
 * be safe to run more than once over the same model.
 */
class JavaZdlModelAnnotationsTest {

    /** Minimal {@link Annotated}, so scoping is tested without dragging in a whole ZDL model. */
    record Element(List<Annotation> annotations) implements Annotated {
        Element() {
            this(new ArrayList<>());
        }
    }

    // ── Annotation.appliesTo ──────────────────────────────────────────────────

    @Test
    void unscopedAnnotationAppliesToEveryArtifact() {
        // Annotation.of leaves artifactTypes empty
        var annotation = Annotation.of("com.acme.Marker");
        Assertions.assertTrue(annotation.appliesTo("domain.entity"));
        Assertions.assertTrue(annotation.appliesTo("outbound.repository-port"));
        Assertions.assertTrue(annotation.appliesTo(null));
    }

    @Test
    void nullArtifactTypesAppliesToEveryArtifact() {
        var annotation = new Annotation("com.acme.Marker", null, null, null);
        Assertions.assertTrue(annotation.appliesTo("domain.entity"));
    }

    @Test
    void scopedAnnotationAppliesOnlyToItsOwnArtifacts() {
        var annotation = Annotation.on("com.acme.Marker", DOMAIN_ENTITY, OUTBOUND_REPOSITORY_PORT);
        Assertions.assertTrue(annotation.appliesTo("domain.entity"));
        Assertions.assertTrue(annotation.appliesTo("outbound.repository-port"));
        Assertions.assertFalse(annotation.appliesTo("inbound.dto"));
    }

    @Test
    void scopedAnnotationDoesNotApplyToAnUntypedArtifact() {
        // a template that declares no artifact type must never render scoped annotations
        var annotation = Annotation.on("com.acme.Marker", DOMAIN_ENTITY);
        Assertions.assertFalse(annotation.appliesTo(null));
    }

    @Test
    void onMapsArtifactTypesToTheirIds() {
        var annotation = Annotation.on("com.acme.Marker", DOMAIN_ENTITY, OUTBOUND_REPOSITORY_PORT);
        Assertions.assertEquals(Set.of("domain.entity", "outbound.repository-port"), annotation.artifactTypes());
    }

    @Test
    void customArtifactTypesAreMatchedOnTheirRawId() {
        // the vocabulary is open: an ad-hoc ArtifactType is first class
        ArtifactType commandHandler = () -> "acme.command-handler";
        var annotation = Annotation.on("com.acme.Marker", commandHandler);
        Assertions.assertTrue(annotation.appliesTo("acme.command-handler"));
        Assertions.assertFalse(annotation.appliesTo("domain.entity"));
    }

    // ── Annotated.addAnnotation ───────────────────────────────────────────────

    @Test
    void addAnnotationIsIdempotent() {
        var element = new Element();
        element.addAnnotation(Annotation.of("com.acme.Marker"));
        element.addAnnotation(Annotation.of("com.acme.Marker"));
        Assertions.assertEquals(1, element.annotations().size());
    }

    @Test
    void addAnnotationKeepsAnnotationsThatDifferOnlyInScope() {
        // same annotation type, different artifact: both must survive
        var element = new Element();
        element.addAnnotation(Annotation.on("com.acme.Marker", DOMAIN_ENTITY));
        element.addAnnotation(Annotation.on("com.acme.Marker", OUTBOUND_REPOSITORY_PORT));
        Assertions.assertEquals(2, element.annotations().size());
    }

    // ── JavaZdlModel.artifactAnnotations ──────────────────────────────────────

    @Test
    void artifactAnnotationsAreIdempotentAndKeyedByArtifactId() {
        var model = new JavaZdlModel(Map.of());
        model.addArtifactAnnotation(INBOUND_DTO, Annotation.of("com.acme.ValueObject"));
        model.addArtifactAnnotation(INBOUND_DTO, Annotation.of("com.acme.ValueObject"));
        model.addArtifactAnnotation(INBOUND_DTO, Annotation.of("com.acme.Other"));

        Assertions.assertEquals(2, model.artifactAnnotations("inbound.dto").size());
        Assertions.assertEquals(List.of(), model.artifactAnnotations("domain.entity"));
    }
}
