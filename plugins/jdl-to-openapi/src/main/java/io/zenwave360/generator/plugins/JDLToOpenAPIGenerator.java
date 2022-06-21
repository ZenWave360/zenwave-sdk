package io.zenwave360.generator.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JDLToOpenAPIGenerator extends AbstractJDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Target file")
    public String targetFile = "openapi.yml";

    public JDLToOpenAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToOpenAPITemplate = new TemplateInput("io/zenwave360/generator/plugins/OpenAPIToJDLGenerator/JDLToOpenAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, ?> getJDLModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        Map<String, ?> jdlModel = getJDLModel(contextModel);
        List<String> serviceNames = JSONPath.get(jdlModel, "$.options.options.service[*].value");
        ((Map) jdlModel).put("serviceNames", serviceNames);

        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new HashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);


        List<Map<String, ?>> entities = (List) JSONPath.get(jdlModel, "$.entities[*]");
        for (Map<String, ?> entity : entities) {
            Map<String, ?> openAPISchema = convertToOpenAPI(entity);
            schemas.put((String) entity.get("name"), openAPISchema);
        }

        List<Map<String, ?>> enums = (List) JSONPath.get(jdlModel, "$.enums.enums[*]");
        for (Map<String, ?> enumValue : enums) {
            Map<String, Object> enumSchema = new LinkedHashMap<>();
            enumSchema.put("type", "string");
            if(enumValue.get("comment") != null) {
                enumSchema.put("description", enumValue.get("comment"));
            }
            List<String> values = JSONPath.get(enumValue, "$.values..name");
            enumSchema.put("enum", values);
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

    private List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob");
    private Map<String,?> convertToOpenAPI(Map<String,?> entity) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if(entity.get("comment") != null) {
            schema.put("description", entity.get("comment"));
        }
        List<String> requiredProperties = new ArrayList<>();
        schema.put("required", requiredProperties);
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.put("properties", properties);

        List<Map<String, ?>> fields = (List) JSONPath.get(entity, "$.fields[*]");
        for (Map<String, ?> field : fields) {
            Map<String, Object> property = new LinkedHashMap<>();

            if("String".equals(field.get("type")) || "TextBlob".equals(field.get("type"))) {
                property.put("type", "string");
            }
            else if("Enum".equals(field.get("type"))) {
                property.put("type", "string");
            }
            else if("LocalDate".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date");
            }
            else if("ZonedDateTime".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            }
            else if("Instant".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            }
            else if("Duration".equals(field.get("type"))) {
                property.put("type", "string");
//                property.put("format", "date-time");
            }
            else if("Integer".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int32");
            }
            else if("Long".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int64");
            }
            else if("Float".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "float");
            }
            else if("Double".equals(field.get("type")) || "BigDecimal".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "double");
            }
            else if("Boolean".equals(field.get("type"))) {
                property.put("type", "boolean");
            }
            else if("UUID".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("pattern", "^[a-f\\d]{4}(?:[a-f\\d]{4}-){4}[a-f\\d]{12}$");
            }
            else if(blobTypes.contains(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "binary");
            } else {
                property.put("$ref", "#/components/schemas/" + field.get("type"));
            }

            String required = JSONPath.get(field, "$.validations.required.value");
            if(required != null) {
                requiredProperties.add((String) field.get("name"));
            }
            String minlength = JSONPath.get(field, "$.validations.minlength.value");
            if(minlength != null) {
                property.put("min-length", minlength);
            }
            String maxlength = JSONPath.get(field, "$.validations.maxlength.value");
            if(maxlength != null) {
                property.put("max-length", maxlength);
            }
            String pattern = JSONPath.get(field, "$.validations.pattern.value");
            if(pattern != null) {
                property.put("pattern", pattern);
            }
            if(field.get("comment") != null){
                property.put("description", field.get("comment"));
            }

            properties.put((String) field.get("name"), property);
        }

        if(requiredProperties.size() == 0) {
            schema.remove("required");
        }

        return schema;
    }

    public TemplateOutput generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, Map<String, ?> jdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdlModel", jdlModel);
        model.put("schemasAsString", schemasAsString);
        return getTemplateEngine().processTemplate(model, template).get(0);
    }

    protected TemplateEngine getTemplateEngine() {
        handlebarsEngine.getHandlebars().registerHelper("asTagName", (context, options) -> {
            if(context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });
        return handlebarsEngine;
    }
}
