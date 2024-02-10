package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.generators.EntitiesToAvroConverter;
import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLToAsyncAPIGenerator extends AbstractZDLGenerator {

    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper jsonMapper = new ObjectMapper();

    enum SchemaFormat {
        schema, avro
    }

    enum PayloadStyle {
        entity, event
    }

    public String sourceProperty = "zdl";

    @DocumentedOption(description = "Target AsyncAPI version.")
    public AsyncapiVersionType asyncapiVersion = AsyncapiVersionType.v2;

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Entities to skip code generation for")
    public List<String> skipEntities = new ArrayList<>();


    @DocumentedOption(description = "Annotations to generate code for (ex. aggregate)")
    public List<String> annotations = new ArrayList<>();
    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> skipForAnnotations = List.of("vo", "embedded", "skip");

    @DocumentedOption(description = "Include channels and messages to publish domain events")
    public boolean includeEvents = true;

    @DocumentedOption(description = "Include channels and messages to listen for async command requests")
    public boolean includeCommands = false;

    @DocumentedOption(description = "Target file")
    public String targetFile = "asyncapi.yml";
    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Schema format for messages' payload")
    public SchemaFormat schemaFormat = SchemaFormat.schema;

    @DocumentedOption(description = "Payload Style for messages' payload")
    public PayloadStyle payloadStyle = PayloadStyle.entity;

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    @DocumentedOption(description = "Package name for generated Avro Schemas (.avsc)")
    public String avroPackage = "io.example.domain.model";

    public String defaultSchemaFormatV2 = "application/vnd.aai.asyncapi;version=2.6.0";
    public String defaultSchemaFormatV3 = "application/vnd.aai.asyncapi;version=3.0.0";
    public String avroSchemaFormat = "application/vnd.apache.avro+json;version=1.9.0";

    public JDLToAsyncAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToAsyncAPITemplate = new TemplateInput("io/zenwave360/sdk/plugins/AsyncAPIToJDLGenerator/JDLToAsyncAPI{{asyncapiVersion}}.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getJDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    protected boolean isGenerateSchemaEntity(Map<String, Object> entity) {
        String entityName = (String) entity.get("name");
        boolean isIncluded = entities.isEmpty() || entities.contains(entityName);
        boolean isExcluded = skipEntities.contains(entityName);
        return isIncluded && !isExcluded;
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> outputList = new ArrayList<>();
        Map<String, Object> zdlModel = getJDLModel(contextModel);
        List<String> serviceNames = JSONPath.get(zdlModel, "$.options.options.service[*].value");
        ((Map) zdlModel).put("serviceNames", serviceNames);

        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        EntitiesToAvroConverter toAvroConverter = new EntitiesToAvroConverter().withIdType(idType).withNamespace(avroPackage);
        EntitiesToSchemasConverter toSchemasConverter = new EntitiesToSchemasConverter().withIdType(idType, idTypeFormat).withZdlBusinessEntityProperty(jdlBusinessEntityProperty);
        toSchemasConverter.includeVersion = false;

        List<Map<String, Object>> entities = (List) JSONPath.get(zdlModel, "$.entities[*]");
        List<Map<String, Object>> enums = (List) JSONPath.get(zdlModel, "$.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        for (Map<String, Object> entity : entitiesAndEnums) {
            if (!isGenerateSchemaEntity(entity)) {
                continue;
            }
            if (schemaFormat == SchemaFormat.schema) {
                String entityName = (String) entity.get("name");
                Map<String, Object> asyncAPISchema = toSchemasConverter.convertToSchema(entity, zdlModel);
                schemas.put(entityName, asyncAPISchema);
            }
            if (schemaFormat == SchemaFormat.avro) {
                outputList.addAll(createAvroRequestAndEventTypeEnums(toAvroConverter));
                outputList.addAll(convertToAvro(toAvroConverter, entity, zdlModel));
            }
        }

        String asyncAPISchemasString = "";
        if (schemaFormat == SchemaFormat.schema) {
            asyncAPISchemasString = writeAsString(yamlMapper, oasSchemas);
            // remove first line
            asyncAPISchemasString = asyncAPISchemasString.substring(asyncAPISchemasString.indexOf("components:") + 12);
        }

        outputList.add(generateTemplateOutput(contextModel, jdlToAsyncAPITemplate, zdlModel, asyncAPISchemasString));
        return outputList;
    }

    protected String writeAsString(ObjectMapper mapper, Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<TemplateOutput> createAvroRequestAndEventTypeEnums(EntitiesToAvroConverter converter) {
        List<TemplateOutput> outputList = new ArrayList<>();
        String targetFolder = getTargetAvroFolder();
        if(includeCommands) {
            Map enumEntity = Map.of("type", "enum", "name", "RequestType", "values", Map.of("create", Map.of("name", "create"), "update", Map.of("name", "update"), "delete", Map.of("name", "delete")));
            String avroString = writeAsString(jsonMapper, converter.convertEnumToAvro(enumEntity));
            outputList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, "RequestType"), avroString, OutputFormatType.JSON.toString()));
        }
        if(includeEvents) {
            Map enumEntity = Map.of("type", "enum", "name", "EventType", "values", Map.of("created", Map.of("name", "created"), "updated", Map.of("name", "updated"), "deleted", Map.of("name", "deleted")));
            String avroString = writeAsString(jsonMapper, converter.convertEnumToAvro(enumEntity));
            outputList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, "EventType"), avroString, OutputFormatType.JSON.toString()));
        }
        return outputList;
    }

    protected String getTargetAvroFolder() {
        String targetFolder = new File(targetFile).getParent();
        return targetFolder == null ? "avro" : targetFolder + "/avro";
    }

    protected List<TemplateOutput> convertToAvro(EntitiesToAvroConverter converter, Map<String, Object> entityOrEnum, Map<String, Object> zdlModel) {
        String name = (String) entityOrEnum.get("name");
        Map avro = converter.convertToAvro(entityOrEnum, zdlModel);
        String avroJson = writeAsString(jsonMapper, avro);
        String targetFolder = getTargetAvroFolder();
        List<TemplateOutput> avroList = new ArrayList<>();

        avroList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, name), avroJson, OutputFormatType.JSON.toString()));

        if (payloadStyle == PayloadStyle.event && (!skipOperations(entityOrEnum) || entityOrEnum.get("fields") == null)) {
            // creating 'fake' jdl entities for message payloads for created/updated/deleted as { id: <id>, payload: <entity> }

            Map<String, Object> fields = new HashMap<>();
            fields.put("payload", Map.of("isEntity", true, "isEnum", false, "name", "payload", "type", name));

            if(includeCommands) {
                fields.put("eventType", Map.of("isEntity", false, "isEnum", true, "name", "requestType", "type", "RequestType"));

                Map<String, Object> requestPayload = new HashMap<>();
                requestPayload.put("name", name + "RequestPayload");
                requestPayload.put("fields", fields);
                avroJson = writeAsString(jsonMapper, converter.convertToAvro(requestPayload, zdlModel));
                avroList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, name + "RequestPayload"), avroJson, OutputFormatType.JSON.toString()));
            }

            if(includeEvents) {
                fields.put("eventType", Map.of("isEntity", false, "isEnum", true, "name", "eventType", "type", "EventType"));

                Map<String, Object> eventPayload = new HashMap<>();
                eventPayload.put("name", name + "EventPayload");
                eventPayload.put("fields", fields);
                avroJson = writeAsString(jsonMapper, converter.convertToAvro(eventPayload, zdlModel));
                avroList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, name + "EventPayload"), avroJson, OutputFormatType.JSON.toString()));
            }
        }

        return avroList;
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        model.put("schemaFormatString", schemaFormat == SchemaFormat.schema ? (asyncapiVersion == AsyncapiVersionType.v3? defaultSchemaFormatV3 : defaultSchemaFormatV2) : avroSchemaFormat);
        model.put("schemasAsString", schemasAsString);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }

    protected boolean skipOperations(Map entity) {
        if (!isGenerateSchemaEntity(entity)) {
            return true;
        }
        String annotationsFilter = annotations.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
        boolean hasAnnotation = !annotations.isEmpty() && !JSONPath.get(entity, "$.options[?(" + annotationsFilter + ")]", Collections.emptyList()).isEmpty();
        if(!annotations.isEmpty()) {
            return !hasAnnotation;
        }

        String skipAnnotationsFilter = skipForAnnotations.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
        boolean hasSkipAnnotation = !JSONPath.get(entity, "$.options[?(" + skipAnnotationsFilter + ")]", Collections.emptyList()).isEmpty();
        return skipForAnnotations.isEmpty() || hasSkipAnnotation;
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("skipOperations", (context, options) -> {
            return skipOperations((Map) context);
        });

        handlebarsEngine.getHandlebars().registerHelper("asTagName", (context, options) -> {
            if (context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });

        handlebarsEngine.getHandlebars().registerHelper("isStateTransferPayloadStyle", (context, options) -> {
            return payloadStyle == PayloadStyle.event;
        });

        handlebarsEngine.getHandlebars().registerHelper("payloadRef", (context, options) -> {
            Map entity = (Map) context;
            String messageType = payloadStyle == PayloadStyle.event ? options.param(0) : "";
            String payloadStyleSuffix = payloadStyle == PayloadStyle.event ? "Payload" : "";
            if (schemaFormat == SchemaFormat.avro) {
                return String.format("avro/%s%s%s.avsc", entity.get("className"), messageType, payloadStyleSuffix);
            }
            return String.format("#/components/schemas/%s%s%s", entity.get("className"), messageType, payloadStyleSuffix);
        });
    }
}
