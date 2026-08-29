package io.zenwave360.sdk.zdl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.annotators.ArtifactType;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaZdlModel {

    public Map<String, Object> zdlModel;
    public String idJavaType;

    public List<Entity> entities = new ArrayList<>();
    public List<Service> services = new ArrayList<>();
    public List<Enum> enums = new ArrayList<>();
    public List<Input> inputs = new ArrayList<>();
    public List<Output> outputs = new ArrayList<>();
    public List<Event> events = new ArrayList<>();
    public List<Event> externalEvents = new ArrayList<>();

    /**
     * Annotations for generated artifacts that have no backing ZDL element, keyed by
     * {@link ArtifactType#id()}. Rendered by {@code {{annotate 'some.artifact-type'}}} when the
     * helper is called without a model node.
     */
    public Map<String, List<Annotation>> artifactAnnotations = new LinkedHashMap<>();

    /**
     * Which architecture vocabulary the jMolecules annotator actually applied, so templates that
     * generate architecture *verification* (rather than annotations) can match it. Null when
     * jMolecules is off.
     */
    public String jmoleculesArchitecture;

    public JavaZdlModel(Map<String, Object> zdlModel) {
        this(zdlModel, "String");
    }

    public JavaZdlModel(Map<String, Object> zdlModel, String idJavaType) {
        this.zdlModel = zdlModel;
        this.idJavaType = idJavaType;
        for (Map entity : JSONPath.get(zdlModel, "$.entities[*]", List.<Map>of())) {
            entities.add(createEntity(entity));
        }
        for (Map zdlEnum : JSONPath.get(zdlModel, "$.enums[*]", List.<Map>of())) {
            enums.add(createEnum(zdlEnum));
        }
        for (Map input : JSONPath.get(zdlModel, "$.inputs[*]", List.<Map>of())) {
            inputs.add(createInput(input));
        }
        for (Map output : JSONPath.get(zdlModel, "$.outputs[*]", List.<Map>of())) {
            outputs.add(createOutput(output));
        }
        for (Map service : JSONPath.get(zdlModel, "$.services[*]", List.<Map>of())) {
            services.add(createService(service));
        }
        for (Map event : JSONPath.get(zdlModel, "$.events[*][?(!@.options.embedded)]", List.<Map>of())) {
            var isExternal = JSONPath.get(event, "$.options.asyncapi") != null;
            if (isExternal) {
                externalEvents.add(createEvent(event));
            } else {
                events.add(createEvent(event));
            }
        }
    }

    /**
     * Adds an annotation for an artifact that has no backing ZDL element. Idempotent: contributing
     * the same annotation twice is a no-op, so annotators may safely run more than once over the
     * same model.
     */
    public void addArtifactAnnotation(ArtifactType artifactType, Annotation annotation) {
        var annotations = artifactAnnotations.computeIfAbsent(artifactType.id(), key -> new ArrayList<>());
        if (!annotations.contains(annotation)) {
            annotations.add(annotation);
        }
    }

    public List<Annotation> artifactAnnotations(String artifactType) {
        return artifactAnnotations.getOrDefault(artifactType, List.of());
    }

    public Entity createEntity(Map<String, Object> entity) {
        var javaEntity = new Entity(
                (String) entity.get("name"),
                (String) entity.get("comment"),
                createIdField(entity),
                JSONPath.get(entity, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                JSONPath.get(entity, "$.relationships[*]", List.<Map>of()).stream().map(this::createRelationship).toList(),
                new ArrayList<>()
        );
        entity.put("javaEntity", javaEntity);
        return javaEntity;
    }

    /**
     * The technical {@code id} is injected by the templates and is not a ZDL field, so it gets a
     * synthetic model element for annotators to target (jMolecules {@code @Identity}, for example).
     * Returns {@code null} for entities the templates render without an id.
     */
    public Field createIdField(Map<String, Object> entity) {
        var hasNoId = !JSONPath.get(entity, "$.options[?(@.embedded || @.vo || @.input || @.abstract)]", List.of()).isEmpty();
        if (hasNoId) {
            return null;
        }
        return new Field("id", null, idJavaType, new ArrayList<>());
    }

    public Enum createEnum(Map<String, Object> zdlEnum) {
        var javaEnum = new Enum(
                (String) zdlEnum.get("name"),
                (String) zdlEnum.get("comment"),
                JSONPath.get(zdlEnum, "$.values[*]", List.<Map>of()).stream().map(this::createEnumValue).toList(),
                new ArrayList<>()
        );
        zdlEnum.put("javaEnum", javaEnum);
        return javaEnum;
    }

    public EnumValue createEnumValue(Map<String, Object> map) {
        var enumValue = new EnumValue(
                (String) map.get("name"),
                (String) map.get("comment"),
                (String) map.get("value"),
                new ArrayList<>()
        );
        map.put("javaEnumValue", enumValue);
        return enumValue;
    }

    public Input createInput(Map<String, Object> input) {
        var javaInput = new Input(
                (String) input.get("name"),
                (String) input.get("comment"),
                JSONPath.get(input, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
        input.put("javaInput", javaInput);
        return javaInput;
    }

    public Output createOutput(Map<String, Object> output) {
        var javaOutput = new Output(
                (String) output.get("name"),
                (String) output.get("comment"),
                JSONPath.get(output, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
        output.put("javaOutput", javaOutput);
        return javaOutput;
    }

    public Event createEvent(Map<String, Object> event) {
        var javaEvent = new Event(
                (String) event.get("name"),
                (String) event.get("comment"),
                JSONPath.get(event, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
        event.put("javaEvent", javaEvent);
        return javaEvent;
    }

    public Field createField(Map<String, Object> map) {
        var javaField = new Field(
                (String) map.get("name"),
                (String) map.get("comment"),
                (String) map.get("type"),
                new ArrayList<>()
        );
        map.put("javaField", javaField);
        return javaField;
    }

    public Relationship createRelationship(Map<String, Object> map) {
        var javaRelationship = new Relationship(
                (String) map.get("fieldName"),
                (String) map.get("comment"),
                (String) map.get("type"),
                (String) map.get("entityName"),
                (String) map.get("otherEntityName"),
                JSONPath.get(map, "$.ownerSide", false),
                JSONPath.get(map, "$.isCollection", false),
                new ArrayList<>()
        );
        map.put("javaRelationship", javaRelationship);
        return javaRelationship;
    }

    public Service createService(Map<String, Object> service) {
        var javaService = new Service(
                (String) service.get("name"),
                (String) service.get("comment"),
                JSONPath.get(service, "$.methods[*]", List.<Map>of()).stream().map(this::createServiceMethod).toList(),
                new ArrayList<>()
        );
        service.put("javaService", javaService);
        return javaService;
    }

    public ServiceMethod createServiceMethod(Map<String, Object> map) {
        var serviceMethod = new ServiceMethod(
                (String) map.get("name"),
                (String) map.get("comment"),
                createServiceMethodParameters(map),
                createServiceMethodReturnType(map),
                new ArrayList<>()
        );
        map.put("javaServiceMethod", serviceMethod);
        return serviceMethod;
    }

    public List<MethodParameter> createServiceMethodParameters(Map<String, Object> method) {
        var params = new ArrayList<MethodParameter>();
        if(JSONPath.get(method, "paramId") != null) {
            var hasNaturalId = JSONPath.get(method, "naturalId", false);
            var paramIdIsOptional = JSONPath.get(method, "paramIdIsOptional", false);
            if (hasNaturalId) {
                var fields = ZDLFindUtils.naturalIdFields(JSONPath.get(zdlModel, "$.entities." + method.get("entity")));
                for (var field : fields) {
                    params.add(new MethodParameter((String) field.get("name"), (String) field.get("type"), MethodParameterType.NATURAL_ID,false, paramIdIsOptional, new ArrayList<>()));
                }
            } else {
                params.add(new MethodParameter("id", idJavaType, MethodParameterType.ID,false, paramIdIsOptional, new ArrayList<>()));
            }
        }
        var parameterType = (String) method.get("parameter");
        var parameterEntity = (Map) JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + parameterType);
        if(JSONPath.get(method, "parameter") != null) {
            var isInline = Boolean.TRUE.equals(JSONPath.get(parameterEntity, "$.options.inline", false));
            var fields = (Map<String, Map>) JSONPath.get(parameterEntity, "$.fields");
            if (isInline && fields != null && !fields.isEmpty()) {
                for (var field : fields.values()) {
                    var isArray = JSONPath.get(field, "$.isArray", false);
                    var isRequired = JSONPath.get(field, "validations.required") != null;
                    params.add(new MethodParameter((String) field.get("name"), (String) field.get("type"), MethodParameterType.PARAMETER, isArray, !isRequired, new ArrayList<>()));
                }
            } else {
                var methodParameterType = ZDLJavaSignatureUtils.methodParameterType(method, zdlModel);
                var isOptional = JSONPath.get(method, "parameterIsOptional", false);
                params.add(new MethodParameter("input", methodParameterType, MethodParameterType.PARAMETER,false, isOptional, new ArrayList<>()));
            }
        }
        if(JSONPath.get(method, "options.paginated") != null) {
            params.add(new MethodParameter("pageable", "Pageable", MethodParameterType.PAGINATION,false, false, new ArrayList<>()));
        }
        return params;
    }

    public ReturnType createServiceMethodReturnType(Map<String, Object> method) {
        var returnType = (String) method.get("returnType");
        if(returnType == null) {
            return null;
        }
        return new ReturnType(
                returnType,
                (Boolean) method.getOrDefault("returnTypeIsArray", false),
                (Boolean) method.getOrDefault("returnTypeIsOptional", false),
                new ArrayList<>());
    }


    /**
     * Implemented by every model element that can carry annotations, so the {@code {{annotate}}}
     * helper works generically instead of switching on record types.
     */
    public interface Annotated {

        List<Annotation> annotations();

        /**
         * Adds an annotation unless an equal one is already present. {@link Annotation} is a record,
         * so equality is structural and this makes annotator contribution idempotent: running
         * annotators twice over the same model never accumulates duplicates.
         */
        default void addAnnotation(Annotation annotation) {
            if (!annotations().contains(annotation)) {
                annotations().add(annotation);
            }
        }
    }

    /**
     * A Java annotation to render on a generated element.
     *
     * @param name          fully qualified annotation type name
     * @param value         raw, pre-formatted attribute text, or null
     * @param options       reserved for structured attributes
     * @param artifactTypes the artifact types this annotation applies to. Empty means it renders in
     *                      every artifact that renders the owning element.
     */
    public record Annotation(String name, String value, Map<String, Object> options, Set<String> artifactTypes) {

        public Annotation(String name, String value, Map<String, Object> options) {
            this(name, value, options, Set.of());
        }

        public boolean appliesTo(String artifactType) {
            return artifactTypes == null || artifactTypes.isEmpty()
                    || (artifactType != null && artifactTypes.contains(artifactType));
        }

        /** Artifact independent: renders wherever the owning element renders. */
        public static Annotation of(String name) {
            return new Annotation(name, null, null, Set.of());
        }

        /** Scoped to the given artifact types. */
        public static Annotation on(String name, ArtifactType... artifactTypes) {
            return new Annotation(name, null, null,
                    Arrays.stream(artifactTypes).map(ArtifactType::id).collect(Collectors.toSet()));
        }
    }

    public record Entity(String name, String comment, Field idField, List<Field> fields, List<Relationship> relationships, List<Annotation> annotations) implements Annotated {
    }

    public record Field(String name, String comment, String type, List<Annotation> annotations) implements Annotated {
    }

    public record Relationship(String name, String comment, String type, String entityName, String otherEntityName, boolean ownerSide, boolean isCollection, List<Annotation> annotations) implements Annotated {
    }

    public record Service(String name, String comment, List<ServiceMethod> methods, List<Annotation> annotations) implements Annotated {
    }

    public record ServiceMethod(String name, String comment, List<MethodParameter> parameters, ReturnType returnType, List<Annotation> annotations) implements Annotated {
    }

    public enum MethodParameterType {
        ID, NATURAL_ID, PARAMETER, PAGINATION
    }

    public record MethodParameter(String name, String type, MethodParameterType parameterType, boolean isArray, boolean isOptional, List<Annotation> annotations) implements Annotated {
    }

    public record ReturnType(String type, boolean isArray, boolean isOptional, List<Annotation> annotations) implements Annotated {
    }

    public record Enum(String name, String comment, List<EnumValue> values, List<Annotation> annotations) implements Annotated {
    }

    public record EnumValue(String name, String comment, String value, List<Annotation> annotations) implements Annotated {
    }

    public record Input(String name, String comment, List<Field> fields, List<Annotation> annotations) implements Annotated {
    }

    public record Output(String name, String comment, List<Field> fields, List<Annotation> annotations) implements Annotated {
    }

    public record Event(String name, String comment, List<Field> fields, List<Annotation> annotations) implements Annotated {
    }
}
