package io.zenwave360.generator.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractJDLGenerator;
import io.zenwave360.generator.generators.JDLEntitiesToSchemasConverter;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;

public class JDLToOpenAPIGenerator extends AbstractJDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> annotationsToGenerate = List.of("aggregate");
    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> skipForAnnotations = List.of("vo", "embedded", "skip");

    @DocumentedOption(description = "Target file")
    public String targetFile = "openapi.yml";
    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas for paginated lists")
    public String jdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. Examples: '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.items", "$.properties.content.items");

    @DocumentedOption(description = "Suffix for search criteria DTOs (default: Criteria)")
    public String criteriaDTOSuffix = "Criteria";

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    public JDLToOpenAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToOpenAPITemplate = new TemplateInput("io/zenwave360/generator/plugins/OpenAPIToJDLGenerator/JDLToOpenAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getJDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    protected boolean isGenerateSchemaEntity(Map<String, Object> entity) {
        String entityName = (String) entity.get("name");
        return entities.isEmpty() || entities.contains(entityName);
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("skipOperations", (context, options) -> {
            Map entity = (Map) context;
            String annotationsFilter = annotationsToGenerate.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
            String skipAnnotationsFilter = skipForAnnotations.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
            boolean isGenerate = annotationsToGenerate.isEmpty() || !((List) JSONPath.get(entity, "$.options[?(" + annotationsFilter + ")]")).isEmpty();
            boolean isSkip = skipForAnnotations.isEmpty() || !((List) JSONPath.get(entity, "$.options[?(" + skipAnnotationsFilter + ")]")).isEmpty();
            return !isGenerate || isSkip;
        });
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Map<String, Object> jdlModel = getJDLModel(contextModel);
        List<String> serviceNames = JSONPath.get(jdlModel, "$.options.options.service[*].value");
        ((Map) jdlModel).put("serviceNames", serviceNames);

        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        JDLEntitiesToSchemasConverter converter = new JDLEntitiesToSchemasConverter().withIdType(idType, idTypeFormat).withJdlBusinessEntityProperty(jdlBusinessEntityProperty);

        List<Map<String, Object>> entities = (List) JSONPath.get(jdlModel, "$.entities[*]");
        for (Map<String, Object> entity : entities) {
            if (!isGenerateSchemaEntity(entity)) {
                continue;
            }
            String entityName = (String) entity.get("name");
            Map<String, Object> openAPISchema = converter.convertToSchema(entity, jdlModel);
            schemas.put(entityName, openAPISchema);

            Map<String, Object> paginatedSchema = new HashMap<>();
            paginatedSchema.put("allOf", List.of(
                    Map.of("$ref", "#/components/schemas/Page"),
                    Map.of(jdlBusinessEntityPaginatedProperty, entityName),
                    Map.of("properties",
                            Map.of("content",
                                    Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName))))));
            schemas.put(entityName + "Paginated", paginatedSchema);
        }

        List<Map<String, Object>> enums = (List) JSONPath.get(jdlModel, "$.enums.enums[*]");
        for (Map<String, Object> enumValue : enums) {
            if (!isGenerateSchemaEntity(enumValue)) {
                continue;
            }
            Map<String, Object> enumSchema = converter.convertToSchema(enumValue, jdlModel);
            schemas.put((String) enumValue.get("name"), enumSchema);
        }

        String openAPISchemasString = null;
        try {
            openAPISchemasString = mapper.writeValueAsString(oasSchemas);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // remove first line
        openAPISchemasString = openAPISchemasString.substring(openAPISchemasString.indexOf("\n") + 1);

        return List.of(generateTemplateOutput(contextModel, jdlToOpenAPITemplate, jdlModel, openAPISchemasString));
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> jdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdlModel", jdlModel);
        model.put("schemasAsString", schemasAsString);
        return getTemplateEngine().processTemplate(model, template).get(0);
    }

    protected TemplateEngine getTemplateEngine() {
        handlebarsEngine.getHandlebars().registerHelper("asTagName", (context, options) -> {
            if (context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });
        return handlebarsEngine;
    }
}
