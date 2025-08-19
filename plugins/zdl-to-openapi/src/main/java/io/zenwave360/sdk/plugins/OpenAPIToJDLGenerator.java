package io.zenwave360.sdk.plugins;

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public class OpenAPIToJDLGenerator extends AbstractZDLGenerator {

    public String sourceProperty = "api";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Target file")
    public String targetFile;

    @DocumentedOption(description = "Whether to use JDL relationships or plain field")
    public boolean useRelationships = true;

    public OpenAPIToJDLGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput openAPIToJDLTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZDLToOpenAPIGenerator/OpenAPIToJDL.jdl", "{{targetFile}}").withMimeType(OutputFormatType.JDL);

    protected Map<String, Object> getOpenAPIModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Map<String, Object> openAPIModel = getOpenAPIModel(contextModel);

        Map<String, Object> zdlModel = new HashMap<>();
        List<Map<String, Object>> entities = new ArrayList<>();
        List<Map<String, Object>> enums = new ArrayList<>();
        zdlModel.put("entities", entities);
        zdlModel.put("enums", enums);
        Map<String, Map> schemas = JSONPath.get(openAPIModel, "$.components.schemas");
        for (Map.Entry<String, Map> schemaEntry : schemas.entrySet()) {
            var entity = new HashMap<String, Object>();
            entity.put("name", schemaEntry.getKey());
            entity.put("description", schemaEntry.getValue().get("description"));
            if ("object".equals(schemaEntry.getValue().get("type"))) {
                var fields = new ArrayList<Map<String, Object>>();
                entity.put("fields", fields);
                var properties = (Map<String, Map>) schemaEntry.getValue().get("properties");
                if (properties == null) {
                    continue;
                }
                for (Map.Entry<String, Map> propertyEntry : properties.entrySet()) {
                    if ("id".equals(propertyEntry.getKey())) {
                        continue;
                    }
                    var field = new HashMap<String, Object>();
                    field.put("name", propertyEntry.getKey());
                    field.put("description", propertyEntry.getValue().get("description"));
                    field.put("type", getJDLType(schemaEntry.getKey(), propertyEntry.getValue()));
                    if (field.get("type") != null) { // this would be a relationship
                        fields.add(field);
                    }
                }
                entities.add(entity);
            }
            if ("string".equals(schemaEntry.getValue().get("type"))) {
                entity.put("enumValues", schemaEntry.getValue().get("enum"));
                enums.add(entity);
            }
        }

        for (var enumEntry : inlineEnums.entrySet()) {
            var entity = new HashMap<String, Object>();
            entity.put("name", enumEntry.getKey());
            entity.put("description", enumEntry.getValue().get("description"));
            entity.put("enumValues", enumEntry.getValue().get("enum"));
            enums.add(entity);
        }

        var relationships = new HashMap<>();
        relationships.put("oneToMany", oneToMany);
        relationships.put("manyToOne", manyToOne);
        zdlModel.put("relationships", relationships);

        var generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, openAPIToJDLTemplate, zdlModel));
        return generatedProjectFiles;
    }

    private Map<String, Map<String, Object>> inlineEnums = new HashMap<>();
    private List<String[]> manyToOne = new ArrayList<>();
    private List<String[]> oneToMany = new ArrayList<>();

    protected String getJDLType(String entityName, Map<String, Object> property) {
        String type = (String) property.get("type");
        String format = (String) property.get("format");
        if ("date".equals(format)) {
            return "LocalDate";
        }
        if ("date-time".equals(format)) {
            return "Instant";
        }
        if ("integer".equals(type) && "int32".equals(format)) {
            return "Integer";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "Long";
        }
        if ("number".equals(type)) {
            return "BigDecimal";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if ("array".equals(type)) {
            var items = (Map<String, Object>) property.get("items");
            var propertyName = (String) property.get("x--property-name");
            String itemsJDLType = getJDLType(entityName, items);
            if (useRelationships) {
                oneToMany.add(new String[] {entityName, propertyName, itemsJDLType});
                return null;
            } else {
                return String.format("%s[]", itemsJDLType);
            }
        }
        if (property.get("x--schema-name") != null) {
            // root level #/component/schemas would be an entity or enum
            String otherEntity = (String) property.get("x--schema-name");
            String propertyName = (String) property.get("x--property-name");
            if (useRelationships && propertyName != null && property.get("enum") == null) {
                manyToOne.add(new String[] {entityName, propertyName, otherEntity});
                return null;
            } else {
                return (String) property.get("x--schema-name");
            }
        }
        if (property.get("enum") != null) { // inline enum
            String inLineEnumName = capitalize(entityName) + capitalize((String) property.get("x--property-name"));
            inlineEnums.put(inLineEnumName, property);
            return inLineEnumName;
        }
        return "String";
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        return getTemplateEngine().processTemplate(model, template);
    }



}
