package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;

import java.util.Map;

/**
 * Contributes Java annotations to the elements of a {@link JavaZdlModel}, without annotation packs
 * having to be hard-coded into Handlebars templates.
 *
 * <p>
 * Annotators run once, right after the model is built, and mutate it. Contribution is idempotent
 * (see {@link JavaZdlModel.Annotated#addAnnotation}), so running annotators more than once over the
 * same model never accumulates duplicates.
 * </p>
 *
 * <p>
 * Annotations may be scoped to the generated artifacts they belong to, which is how one ZDL element
 * can be annotated differently in each of the files it generates:
 * </p>
 *
 * <pre>{@code
 * entity.addAnnotation(Annotation.on("org.jmolecules.ddd.annotation.AggregateRoot", DOMAIN_ENTITY));
 * entity.addAnnotation(Annotation.on("org.jmolecules.ddd.annotation.Repository", OUTBOUND_REPOSITORY_PORT));
 * }</pre>
 */
public interface ZDLAnnotator {

    default void annotate(JavaZdlModel javaModel, Map<String, Object> zdl) {
        for (JavaZdlModel.Entity entity : javaModel.entities) {
            var zdlEntity = JSONPath.get(zdl, "$.entities." + entity.name(), Map.<String, Object>of());
            annotate(entity, zdlEntity, zdl);
            if (entity.idField() != null) {
                annotate(entity.idField(), entity, zdlEntity, zdl);
            }
            for (JavaZdlModel.Field field : entity.fields()) {
                annotate(field, entity, zdlEntity, zdl);
            }
            for (JavaZdlModel.Relationship relationship : entity.relationships()) {
                annotate(relationship, entity, zdlEntity, zdl);
            }
        }

        for (JavaZdlModel.Enum javaEnum : javaModel.enums) {
            var zdlEnum = JSONPath.get(zdl, "$.enums." + javaEnum.name(), Map.<String, Object>of());
            annotate(javaEnum, zdlEnum, zdl);
            for (JavaZdlModel.EnumValue value : javaEnum.values()) {
                annotate(value, javaEnum, zdlEnum, zdl);
            }
        }

        for (JavaZdlModel.Input input : javaModel.inputs) {
            var zdlInput = JSONPath.get(zdl, "$.inputs." + input.name(), Map.<String, Object>of());
            annotate(input, zdlInput, zdl);
            for (JavaZdlModel.Field field : input.fields()) {
                annotate(field, input, zdlInput, zdl);
            }
        }

        for (JavaZdlModel.Output output : javaModel.outputs) {
            var zdlOutput = JSONPath.get(zdl, "$.outputs." + output.name(), Map.<String, Object>of());
            annotate(output, zdlOutput, zdl);
            for (JavaZdlModel.Field field : output.fields()) {
                annotate(field, output, zdlOutput, zdl);
            }
        }

        for (JavaZdlModel.Event event : javaModel.events) {
            annotateEvent(event, zdl);
        }
        for (JavaZdlModel.Event event : javaModel.externalEvents) {
            annotateEvent(event, zdl);
        }

        for (JavaZdlModel.Service service : javaModel.services) {
            var zdlService = JSONPath.get(zdl, "$.services." + service.name(), Map.<String, Object>of());
            annotate(service, zdlService, zdl);

            for (JavaZdlModel.ServiceMethod method : service.methods()) {
                var zdlMethod = JSONPath.get(zdl, "$.services." + service.name() + ".methods." + method.name(), Map.<String, Object>of());
                annotate(method, zdlMethod, zdl);
                for (JavaZdlModel.MethodParameter parameter : method.parameters()) {
                    annotate(parameter, zdlMethod, zdl);
                }
                if (method.returnType() != null) {
                    annotate(method.returnType(), method, zdlMethod, zdl);
                }
            }
        }
    }

    private void annotateEvent(JavaZdlModel.Event event, Map<String, Object> zdl) {
        var zdlEvent = JSONPath.get(zdl, "$.events." + event.name(), Map.<String, Object>of());
        annotate(event, zdlEvent, zdl);
        for (JavaZdlModel.Field field : event.fields()) {
            annotate(field, event, zdlEvent, zdl);
        }
    }

    default void annotate(JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
    }

    /** Fields of entities, inputs, outputs and events, plus the synthetic entity id field. */
    default void annotate(JavaZdlModel.Field field, JavaZdlModel.Annotated owner, Map<String, Object> zdlOwner, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Relationship relationship, JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Enum javaEnum, Map<String, Object> zdlEnum, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.EnumValue enumValue, JavaZdlModel.Enum javaEnum, Map<String, Object> zdlEnum, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Input input, Map<String, Object> zdlInput, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Output output, Map<String, Object> zdlOutput, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Event event, Map<String, Object> zdlEvent, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> method, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> method, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.ReturnType returnType, JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> method, Map<String, Object> zdl) {
    }
}
