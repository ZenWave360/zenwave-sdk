package io.zenwave360.generator.plugins;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.generator.generators.JDLEntitiesToSchemasConverter.convertEntityToSchema;
import static io.zenwave360.generator.generators.JDLEntitiesToSchemasConverter.convertEnumToSchema;

public class JDLToAsyncAPIGenerator extends AbstractJDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> skipForAnnotations = List.of("vo", "embedded", "skip");

    @DocumentedOption(description = "Target file")
    public String targetFile = "asyncapi.yml";
    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    public JDLToAsyncAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToAsyncAPITemplate = new TemplateInput("io/zenwave360/generator/plugins/AsyncAPIToJDLGenerator/JDLToAsyncAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

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
            String annotationsFilter = skipForAnnotations.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
            return skipForAnnotations.isEmpty() || !((List) JSONPath.get(entity, "$.options[?(" + annotationsFilter + ")]")).isEmpty();
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


        List<Map<String, Object>> entities = (List) JSONPath.get(jdlModel, "$.entities[*]");
        for (Map<String, Object> entity : entities) {
            if(!isGenerateSchemaEntity(entity)) {
                continue;
            }
            String entityName = (String) entity.get("name");
            Map<String, Object> asyncAPISchema = convertEntityToSchema(entity, jdlBusinessEntityProperty);
            schemas.put(entityName, asyncAPISchema);
        }

        List<Map<String, Object>> enums = (List) JSONPath.get(jdlModel, "$.enums.enums[*]");
        for (Map<String, Object> enumValue : enums) {
            if(!isGenerateSchemaEntity(enumValue)) {
                continue;
            }
            Map<String, Object> enumSchema = convertEnumToSchema(enumValue);
            schemas.put((String) enumValue.get("name"), enumSchema);
        }

        String asyncAPISchemasString = null;
        try {
            asyncAPISchemasString = mapper.writeValueAsString(oasSchemas);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // remove first line
        asyncAPISchemasString = asyncAPISchemasString.substring(asyncAPISchemasString.indexOf("\n") + 1);

        return List.of(generateTemplateOutput(contextModel, jdlToAsyncAPITemplate, jdlModel, asyncAPISchemasString));
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
            if(context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });
        return handlebarsEngine;
    }
}
