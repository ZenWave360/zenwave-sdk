package io.zenwave360.sdk.zdl.annotators;

/**
 * Identifies the kind of generated artifact a template produces.
 *
 * <p>
 * A template declares its artifact type as the first argument of every annotation emitting helper:
 * </p>
 *
 * <pre>{@code {{annotate 'domain.entity' entity.javaEntity entity}} }</pre>
 *
 * <p>
 * Artifact types name <em>what the template generates</em>, never what the model happens to be: a
 * single template renders aggregate roots, plain entities and value objects alike, and choosing
 * between them is the annotator's job.
 * </p>
 *
 * <p>
 * The vocabulary is open. {@link CoreArtifactType} holds the well known values, while custom
 * templates and annotators may define their own by implementing this interface (an enum, or an
 * ad-hoc lambda such as {@code () -> "acme.command-handler"}). Matching is done on the raw
 * {@link #id()} string, so custom types are first class.
 * </p>
 */
public interface ArtifactType {

    /** The value a template passes to {@code {{annotate}}} and friends. */
    String id();
}
