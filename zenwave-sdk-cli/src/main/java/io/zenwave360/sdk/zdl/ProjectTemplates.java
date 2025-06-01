package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This file will hold all the project templates for a ZDL project generator.
 *
 * <p>
 * Each variable holds the templates for a specific section of the ZDL model.
 * For instance:
 * </p>
 * <ul>
 *   <li>'entityTemplates' holds the templates to be applied for each entity in the ZDL model</li>
 *   <li>'allEntitiesTemplates' holds the templates to be applied **once** passing all entities as context for the template engine</li>
 *   <li>'singleTemplates' holds the templates to be applied **once** for the entire ZDL model</li>
 * </ul>
 */
public class ProjectTemplates {

    protected String templatesFolder;
    protected ProjectLayout layout;

    public String getTemplatesFolder() {
        return templatesFolder;
    }

    public ProjectLayout getLayout() {
        return layout;
    }

    public void setTemplatesFolder(String templatesFolder) {
        this.templatesFolder = templatesFolder;
    }

    public void setLayout(ProjectLayout layout) {
        this.layout = layout;
    }

    public List<Object> getTemplateHelpers(Generator generator) {
        return List.of();
    }

    public List<TemplateInput> aggregateTemplates = new ArrayList<>();

    public List<TemplateInput> domainEventsTemplates = new ArrayList<>();
    public List<TemplateInput> entityTemplates = new ArrayList<>();

    public List<TemplateInput> enumTemplates = new ArrayList<>();
    public List<TemplateInput> inputTemplates = new ArrayList<>();

    public List<TemplateInput> inputEnumTemplates = new ArrayList<>();
    public List<TemplateInput> eventEnumTemplates = new ArrayList<>();
    public List<TemplateInput> outputTemplates = new ArrayList<>();
    public List<TemplateInput> serviceTemplates = new ArrayList<>();

    public List<TemplateInput> externalEventsTemplates = new ArrayList<>();
    public List<TemplateInput> producerTemplates = new ArrayList<>();
    public List<TemplateInput> producerByServiceTemplates = new ArrayList<>();
    public List<TemplateInput> producerByOperationTemplates = new ArrayList<>();

    public List<TemplateInput> consumerTemplates = new ArrayList<>();
    public List<TemplateInput> consumerByServiceTemplates = new ArrayList<>();
    public List<TemplateInput> consumerByOperationTemplates = new ArrayList<>();

    public List<TemplateInput> allEntitiesTemplates = new ArrayList<>();
    public List<TemplateInput> allEnumsTemplates = new ArrayList<>();
    public List<TemplateInput> allInputsTemplates = new ArrayList<>();
    public List<TemplateInput> allOutputsTemplates = new ArrayList<>();
    public List<TemplateInput> allServicesTemplates = new ArrayList<>();
    public List<TemplateInput> allDomainEventsTemplates = new ArrayList<>();
    public List<TemplateInput> allExternalEventsTemplates = new ArrayList<>();
    public List<TemplateInput> singleTemplates = new ArrayList<>();

    public void addTemplate(List<TemplateInput> templates, String sourceFolder, String templateLocation, String targetPackagePlaceholder, String targetFile, OutputFormatType mimeType, Function<Map<String, Object>, Boolean> skip, boolean skipOverwrite) {
        addTemplate(templates, sourceFolder, templateLocation, null, targetPackagePlaceholder, targetFile, mimeType, skip, skipOverwrite);
    }

    public void addTemplate(List<TemplateInput> templates, String sourceFolder, String templateLocation, String targetModule, String targetPackagePlaceholder, String targetFile, OutputFormatType mimeType, Function<Map<String, Object>, Boolean> skip, boolean skipOverwrite) {
        // let the template engine resolve the target package at runtime
        var targetPackage = "{{asPackageFolder layout." + targetPackagePlaceholder + "}}";
        var template = new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, sourceFolder, templateLocation))
                .withTargetFile(joinPath(targetModule, sourceFolder, targetPackage, targetFile))
                .withMimeType(mimeType)
                .withSkipOverwrite(skipOverwrite)
                .withSkip(skip);
        templates.add(template);
    }

    public void removeTemplate(List<TemplateInput> templates, String templateLocation) {
        templates.removeIf(template -> template.getTemplateLocation().equals(templateLocation));
    }

    protected String joinPath(String... paths) {
        var tokens = new ArrayList<>();
        for (String path : paths) {
            if (path != null) {
                tokens.add(path);
            }
        }
        return String.join("/", tokens.toArray(new String[tokens.size()]));
    }
}
