package io.zenwave360.sdk.templating;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the annotations an annotator contributed to a model element, filtered by the artifact the
 * current template generates.
 *
 * <p>
 * The artifact type always comes first, so the same model element can be annotated differently in
 * each file it generates:
 * </p>
 *
 * <pre>{@code
 * {{annotate 'domain.entity' entity.javaEntity entity}}
 * {{annotate 'outbound.repository-port' entity.javaEntity entity}}
 * {{annotate 'domain.entity' entity.javaEntity.idField}}
 * {{annotate 'outbound.event-publisher-port'}}
 * }</pre>
 *
 * <p>
 * Called without a model element, it renders artifact level annotations, for generated files that
 * have no backing ZDL element such as {@code EventPublisher} or {@code package-info}.
 * </p>
 */
public class AnnotationHelper {

    public static String annotate(String artifactType, Options options) {
        return render(annotationsOf(artifactType, options), artifactType);
    }

    private static List<JavaZdlModel.Annotation> annotationsOf(String artifactType, Options options) {
        // elementless means no element argument at all. A null element argument is an absent model
        // node (a value object has no idField, for example) and must render nothing, never the
        // artifact level annotations.
        if (options.params.length == 0) {
            return artifactAnnotations(artifactType, options);
        }
        return options.param(0) instanceof JavaZdlModel.Annotated annotated ? annotated.annotations() : List.of();
    }

    private static List<JavaZdlModel.Annotation> artifactAnnotations(String artifactType, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        var javaModel = zdl != null ? (JavaZdlModel) zdl.get("javaModel") : null;
        return javaModel != null ? javaModel.artifactAnnotations(artifactType) : List.of();
    }

    private static String render(List<JavaZdlModel.Annotation> annotations, String artifactType) {
        return annotations.stream()
                .filter(annotation -> annotation.appliesTo(artifactType))
                .map(AnnotationHelper::render)
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private static String render(JavaZdlModel.Annotation annotation) {
        return annotation.value() != null
                ? "@" + annotation.name() + "(" + annotation.value() + ")"
                : "@" + annotation.name();
    }
}
