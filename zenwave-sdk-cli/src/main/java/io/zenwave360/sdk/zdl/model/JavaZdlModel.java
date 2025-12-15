package io.zenwave360.sdk.zdl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public Entity createEntity(Map<String, Object> entity) {
        return new Entity(
                (String) entity.get("name"),
                (String) entity.get("comment"),
                JSONPath.get(entity, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                JSONPath.get(entity, "$.relationships[*]", List.<Map>of()).stream().map(this::createRelationship).toList(),
                new ArrayList<>()
        );
    }

    public Enum createEnum(Map<String, Object> zdlEnum) {
        return new Enum(
                (String) zdlEnum.get("name"),
                (String) zdlEnum.get("comment"),
                JSONPath.get(zdlEnum, "$.values[*]", List.<Map>of()).stream().map(this::createEnumValue).toList(),
                new ArrayList<>()
        );
    }

    public EnumValue createEnumValue(Map<String, Object> map) {
        return new EnumValue(
                (String) map.get("name"),
                (String) map.get("comment"),
                (String) map.get("value"),
                new ArrayList<>()
        );
    }

    public Input createInput(Map<String, Object> input) {
        return new Input(
                (String) input.get("name"),
                (String) input.get("comment"),
                JSONPath.get(input, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
    }

    public Output createOutput(Map<String, Object> output) {
        return new Output(
                (String) output.get("name"),
                (String) output.get("comment"),
                JSONPath.get(output, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
    }

    public Event createEvent(Map<String, Object> event) {
        return new Event(
                (String) event.get("name"),
                (String) event.get("comment"),
                JSONPath.get(event, "$.fields[*]", List.<Map>of()).stream().map(this::createField).toList(),
                new ArrayList<>()
        );
    }

    public Field createField(Map<String, Object> map) {
        return new Field(
                (String) map.get("name"),
                (String) map.get("comment"),
                (String) map.get("type"),
                new ArrayList<>()
        );
    }

    public Relationship createRelationship(Map<String, Object> map) {
        return new Relationship(null, null, null, null, null, false, false, new ArrayList<>());
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
            if (hasNaturalId) {
                var fields = ZDLFindUtils.naturalIdFields(JSONPath.get(zdlModel, "$.entities." + method.get("entity")));
                for (var field : fields) {
                    params.add(new MethodParameter((String) field.get("name"), (String) field.get("type"), false, false, new ArrayList<>()));
                }
            } else {
                params.add(new MethodParameter("id", idJavaType, false, false, new ArrayList<>()));
            }
        }
        var parameterType = (String) method.get("parameter");
        var parameterEntity = (Map) JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + parameterType);
        if(JSONPath.get(method, "parameter") != null) {
            var isInline = JSONPath.get(parameterEntity, "$.options.inline", false);
            var fields = (Map<String, Map>) JSONPath.get(parameterEntity, "$.fields");
            if (isInline && fields != null && !fields.isEmpty()) {
                for (var field : fields.values()) {
                    var isArray = JSONPath.get(field, "$.isArray", false);
                    var isRequired = JSONPath.get(field, "validations.required") != null;
                    params.add(new MethodParameter((String) field.get("name"), (String) field.get("type"), isArray, !isRequired, new ArrayList<>()));
                }
            } else {
                var methodParameterType = ZDLJavaSignatureUtils.methodParameterType(method, zdlModel);
                var isOptional = JSONPath.get(method, "parameterIsOptional", false);
                params.add(new MethodParameter("input", methodParameterType, false, isOptional, new ArrayList<>()));
            }
        }
        if(JSONPath.get(method, "options.paginated") != null) {
            params.add(new MethodParameter("pageable", "Pageable", false, false, new ArrayList<>()));
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


    public record Annotation(String name, String value, Map<String, Object> options) {
    }

    public record Entity(String name, String comment, List<Field> fields, List<Relationship> relationships, List<Annotation> annotations) {
    }

    public record Field(String name, String comment, String type, List<Annotation> annotations) {
    }

    public record Relationship(String name, String comment, String type, String entityName, String otherEntityName, boolean ownerSide, boolean isCollection, List<Annotation> annotations) {
    }

    public record Service(String name, String comment, List<ServiceMethod> methods, List<Annotation> annotations) {
    }

    public record ServiceMethod(String name, String comment, List<MethodParameter> parameters, ReturnType returnType, List<Annotation> annotations) {
    }

    public record MethodParameter(String name, String type, boolean isArray, boolean isOptional, List<Annotation> annotations) {
    }

    public record ReturnType(String type, boolean isArray, boolean isOptional, List<Annotation> annotations) {
    }

    public record Enum(String name, String comment, List<EnumValue> values, List<Annotation> annotations) {
    }

    public record EnumValue(String name, String comment, String value, List<Annotation> annotations) {
    }

    public record Input(String name, String comment, List<Field> fields, List<Annotation> annotations) {
    }

    public record Output(String name, String comment, List<Field> fields, List<Annotation> annotations) {
    }

    public record Event(String name, String comment, List<Field> fields, List<Annotation> annotations) {
    }
}
