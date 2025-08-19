package io.zenwave360.sdk.generators;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.CommaSeparatedCollectionDeserializationHandler;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ZDLProjectGenerator extends AbstractZDLGenerator {

    public String sourceProperty = "zdl";

    public ProjectLayout layout;
    public ProjectTemplates templates;

    @JsonAnySetter
    public Map<String, Object> options = new LinkedHashMap<>();

    protected Map<String, Object> getZDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        var generatedProjectFiles = generateProjectFiles(contextModel);
        for (TemplateOutput templateOutput : generatedProjectFiles.getAllTemplateOutputs()) {
            var processedTemplateOutput = getTemplateEngine().processTemplate(templateOutput.getContext(), templateOutput.getTemplateInput());
            templateOutput.merge(processedTemplateOutput);
        }
        return generatedProjectFiles;
    }

    public GeneratedProjectFiles generateProjectFiles(Map<String, Object> contextModel) {
        var generatedProjectFiles = new GeneratedProjectFiles();
        var apiModel = getZDLModel(contextModel);

        Map<String, Map<String, Object>> aggregates = (Map) apiModel.get("aggregates");
        Set<Map<String, Object>> domainEvents = new HashSet<>();
        for (Map<String, Object> aggregate : aggregates.values()) {
            for (TemplateInput template : templates.aggregateTemplates) {
                generatedProjectFiles.aggregates.addAll((String) aggregate.get("name"), generateTemplateOutput(contextModel, template, Map.of("aggregate", aggregate)));
            }
            var events = ZDLFindUtils.aggregateEvents(aggregate);
            for (String eventName : events) {
                var event = JSONPath.get(apiModel, "$.events." + eventName);
                if(event != null) {
                    domainEvents.add((Map<String, Object>) event);
                }
            }
        }

        // include all events not annotated with @asyncapi
        domainEvents.addAll((List) JSONPath.get(apiModel, "$.events[*][?(!@.options.asyncapi && !@.options.embedded)]", List.of()));
        // include all events referenced by fields
        JSONPath.get(new ArrayList(domainEvents), "$..fields[*].type", List.of()).stream()
                .map(type -> (Map) JSONPath.get(apiModel, "$.events." + type))
                .filter(Objects::nonNull)
                .forEach(domainEvents::add);

        for (Map<String, Object> domainEvent : domainEvents) {
            for (TemplateInput template : templates.domainEventsTemplates) {
                generatedProjectFiles.domainEvents.addAll((String) domainEvent.get("name"), generateTemplateOutput(contextModel, template, Map.of("event", domainEvent)));
            }
        }


        Map<String, Map<String, Object>> entities = (Map) apiModel.get("entities");
        for (Map<String, Object> entity : entities.values()) {
//            if (!isGenerateEntity(entity)) {
//                continue;
//            }
            for (TemplateInput template : templates.entityTemplates) {
                generatedProjectFiles.entities.addAll((String) entity.get("name"), generateTemplateOutput(contextModel, template, Map.of("entity", entity)));
            }
        }

        Map<String, Map<String, Object>> enums = JSONPath.get(apiModel, "$.enums");
        for (Map<String, Object> enumValue : enums.values()) {
//            if (!isGenerateEntity(enumValue)) {
//                continue;
//            }
            var comment = enumValue.get("comment");
            var isInputEnum = JSONPath.get(enumValue, "$.options.input", false);
            var isEventEnum = JSONPath.get(enumValue, "$.options.event", false);
            if (isInputEnum) {
                for (TemplateInput template : templates.inputEnumTemplates) {
                    generatedProjectFiles.inputEnums.addAll((String) enumValue.get("name"), generateTemplateOutput(contextModel, template, Map.of("enum", enumValue)));
                }
            } else if (isEventEnum) {
                for (TemplateInput template : templates.eventEnumTemplates) {
                    generatedProjectFiles.eventEnums.addAll((String) enumValue.get("name"), generateTemplateOutput(contextModel, template, Map.of("enum", enumValue)));
                }
            } else {
                for (TemplateInput template : templates.enumTemplates) {
                    generatedProjectFiles.enums.addAll((String) enumValue.get("name"), generateTemplateOutput(contextModel, template, Map.of("enum", enumValue)));
                }
            }
        }

        List<Map<String, Object>> inputs = JSONPath.get(apiModel, "$.inputs[*]", Collections.emptyList());
        for (Map<String, Object> input : inputs) {
            for (TemplateInput template : templates.inputTemplates) {
                generatedProjectFiles.inputs.addAll((String) input.get("name"), generateTemplateOutput(contextModel, template, Map.of("entity", input)));
            }
        }
        List<Map<String, Object>> outputs = JSONPath.get(apiModel, "$.outputs[*]", Collections.emptyList());
        for (Map<String, Object> output : outputs) {
            for (TemplateInput template : templates.outputTemplates) {
                generatedProjectFiles.outputs.addAll((String) output.get("name"), generateTemplateOutput(contextModel, template, Map.of("entity", output)));
            }
        }

        // include all internal root events (skip @asyncapi events)
        var eventNames = new HashSet(JSONPath.get(apiModel, "$.services[*].methods[*].withEvents[*]", List.of()));
        var flatEventNames = new ArrayList<String>();
        for (var eventName : eventNames) {
            if (eventName instanceof List eventNamesList) {
                flatEventNames.addAll(eventNamesList);
            } else {
                flatEventNames.add((String) eventName);
            }
        }
        var allExternalEvents = new ArrayList<Map<String, Object>>();
        for (Object eventName : flatEventNames) {
            var event = JSONPath.get(apiModel, "$.events." + eventName);
            if(event != null && JSONPath.get(event, "$.options.asyncapi") == null) {
                allExternalEvents.add((Map<String, Object>) event);
            }
        }

        Map<String, Map<String, Object>> services = JSONPath.get(apiModel, "$.options.options.service", Collections.emptyMap());
        List<Map<String, Object>> servicesList = new ArrayList<>();
        for (Map<String, Object> service : services.values()) {
            String serviceName = ((String) service.get("value"));
            service.put("name", serviceName);
            List<Map<String, Object>> entitiesByService = getEntitiesByService(service, apiModel);
            service.put("entities", entitiesByService);
//            boolean isGenerateService = entitiesByService.stream().anyMatch(entity -> isGenerateEntity(entity));
//            if (!isGenerateService) {
//                continue;
//            }
            servicesList.add(service);
            for (TemplateInput template : templates.serviceTemplates) {
                generatedProjectFiles.services.addAll(serviceName, generateTemplateOutput(contextModel, template, Map.of("service", service, "entities", entitiesByService)));
            }
        }

        if(!entities.isEmpty()) {
            for (TemplateInput template : templates.allEntitiesTemplates) {
                generatedProjectFiles.allEntities.addAll(generateTemplateOutput(contextModel, template, Map.of("entities", new ArrayList(entities.values()))));
            }
        }
        if(!domainEvents.isEmpty()) {
            for (TemplateInput template : templates.allDomainEventsTemplates) {
                generatedProjectFiles.allDomainEvents.addAll(generateTemplateOutput(contextModel, template, Map.of("events", domainEvents.stream().toList())));
            }
        }
        if(!enums.isEmpty()) {
            for (TemplateInput template : templates.allEnumsTemplates) {
                generatedProjectFiles.allEnums.addAll(generateTemplateOutput(contextModel, template, Map.of("enums", new ArrayList(enums.values()))));
            }
        }
        if(!inputs.isEmpty()) {
            for (TemplateInput template : templates.allInputsTemplates) {
                generatedProjectFiles.allInputs.addAll(generateTemplateOutput(contextModel, template, Map.of("inputs", inputs)));
            }
        }
        if(!outputs.isEmpty()) {
            for (TemplateInput template : templates.allOutputsTemplates) {
                generatedProjectFiles.allOutputs.addAll(generateTemplateOutput(contextModel, template, Map.of("outputs", outputs)));
            }
        }
        if(!servicesList.isEmpty()) {
            for (TemplateInput template : templates.allServicesTemplates) {
                generatedProjectFiles.allServices.addAll(generateTemplateOutput(contextModel, template, Map.of("services", servicesList, "entities", new ArrayList(entities.values()))));
            }
        }
        if(!allExternalEvents.isEmpty()) {
            for (TemplateInput template : templates.allExternalEventsTemplates) {
                generatedProjectFiles.allExternalEvents.addAll(generateTemplateOutput(contextModel, template, Map.of("events", allExternalEvents)));
            }
        }


        for (TemplateInput template : templates.singleTemplates) {
            generatedProjectFiles.singleFiles.addAll(generateTemplateOutput(contextModel, template, Collections.emptyMap()));
        }

        return generatedProjectFiles;
    }

    protected List<Map<String, Object>> getEntitiesByService(Map<String, Object> service, Map<String, Object> apiModel) {
        List entityNames = ((List) service.get("entityNames"));
        if (entityNames.size() == 1 && "*".equals(entityNames.get(0))) {
            entityNames = JSONPath.get(apiModel, "$.entities[*].name");
        }
        entityNames = entityNames.stream().map(entity -> JSONPath.get(apiModel, "$.aggregates." + entity + ".aggregateRoot", entity)).toList();
        List<Map<String, Object>> entitiesByService = (List<Map<String, Object>>) entityNames.stream().map(e -> JSONPath.get(apiModel, "$.entities." + e)).toList();
        List excludedNames = ((List) service.get("excludedNames"));
        if (excludedNames != null && excludedNames.size() > 0) {
            entitiesByService = entitiesByService.stream().filter(e -> !excludedNames.contains(e.get("name"))).collect(Collectors.toList());
        }
        service.put("entityNames", entityNames);
        return entitiesByService;
    }

    protected List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(asConfigurationMap());
        model.put("context", contextModel);
        model.put(sourceProperty, getZDLModel(contextModel));
        model.putAll(extModel);
        model.putAll(templates.getDocumentedOptions());
        return getTemplateEngine().processTemplateNames(model, List.of(template));
    }
}
