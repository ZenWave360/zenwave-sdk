package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDLEntitiesGenerator extends AbstractJDLGenerator {

    enum PersistenceType {
        mongodb;
    }

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Persistence MONGODB|JPA default: MONGODB")
    public PersistenceType persistence = PersistenceType.mongodb;


    public JDLEntitiesGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String prefix = "io/zenwave360/generator/plugins/JDLEntitiesGenerator/";
    private final TemplateInput entityTemplate = new TemplateInput(prefix + "{{persistence}}/Entity.java", "{{domainModelPackageFolder}}/{{entity.name}}.java").withMimeType(OutputFormatType.JAVA);
    private final TemplateInput enumTemplate = new TemplateInput(prefix + "common/Enum.java", "{{domainModelPackageFolder}}/{{enum.name}}.java").withMimeType(OutputFormatType.JAVA);

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected Map<String, ?> getJDLModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        var templateOutputList = new ArrayList<TemplateOutput>();
        var apiModel = getJDLModel(contextModel);

        Map<String, Map<String, ?>> entities = (Map) apiModel.get("entities");
        for (Map<String, ?> entity : entities.values()) {
            templateOutputList.add(generateTemplateOutput(contextModel, entityTemplate, "entity",  entity));
        }

        Map<String, Map<String, ?>> enums = JSONPath.get(apiModel, "$.enums.enums");
        for (Map<String, ?> enumValue : enums.values()) {
            templateOutputList.add(generateTemplateOutput(contextModel, enumTemplate, "enum", enumValue));
        }

        return templateOutputList;
    }

    public TemplateOutput generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, String key, Map<String, ?> entity) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdl", getJDLModel(contextModel));
        model.put(key, entity);
        model.put("generator.domainModelPackageFolder", getDomainModelPackageFolder());
        return getTemplateEngine().processTemplate(model, template);
    }

}
