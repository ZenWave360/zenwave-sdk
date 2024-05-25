package io.zenwave360.sdk.generators;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.templating.*;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.ZDLFindUtils;

public abstract class AbstractZDLProjectGenerator extends AbstractZDLGenerator {

    public String sourceProperty = "zdl";

    private final HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected HandlebarsEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected abstract ZDLProjectTemplates configureProjectTemplates();

    protected abstract boolean isGenerateEntity(Map<String, Object> entity);

    protected Map<String, Object> getZDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        ZDLProjectTemplates templates = configureProjectTemplates();

        var templateOutputList = new ArrayList<TemplateOutput>();
        var apiModel = getZDLModel(contextModel);

        Map<String, Map<String, Object>> aggregates = (Map) apiModel.get("aggregates");
        Set<Map<String, Object>> domainEvents = new HashSet<>();
        for (Map<String, Object> aggregate : aggregates.values()) {
            for (TemplateInput template : templates.aggregateTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("aggregate", aggregate)));
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
        JSONPath.get(domainEvents, "$..fields[*].type", List.of()).stream().map(type -> (Map) JSONPath.get(apiModel, "$.events." + type)).filter(Objects::nonNull).forEach(domainEvents::add);

        for (Map<String, Object> domainEvent : domainEvents) {
            for (TemplateInput template : templates.domainEventsTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("event", domainEvent)));
            }
        }


        Map<String, Map<String, Object>> entities = (Map) apiModel.get("entities");
        for (Map<String, Object> entity : entities.values()) {
            if (!isGenerateEntity(entity)) {
                continue;
            }
            for (TemplateInput template : templates.entityTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("entity", entity)));
            }
        }

        Map<String, Map<String, Object>> enums = JSONPath.get(apiModel, "$.enums");
        for (Map<String, Object> enumValue : enums.values()) {
            if (!isGenerateEntity(enumValue)) {
                continue;
            }
            var comment = enumValue.get("comment");
            var isDtoInput = JSONPath.get(enumValue, "$.options.input", false);
            if (isDtoInput) {
                for (TemplateInput template : templates.inputEnumTemplates) {
                    templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("enum", enumValue)));
                }
            } else {
                for (TemplateInput template : templates.enumTemplates) {
                    templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("enum", enumValue)));
                }
            }
        }

        List<Map<String, Object>> inputs = JSONPath.get(apiModel, "$.inputs[*]", Collections.emptyList());
        for (Map<String, Object> input : inputs) {
            for (TemplateInput template : templates.inputTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("entity", input)));
            }
        }
        List<Map<String, Object>> outputs = JSONPath.get(apiModel, "$.outputs[*]", Collections.emptyList());
        for (Map<String, Object> output : outputs) {
            for (TemplateInput template : templates.outputTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("entity", output)));
            }
        }

        Map<String, Map<String, Object>> services = JSONPath.get(apiModel, "$.options.options.service", Collections.emptyMap());
        List<Map<String, Object>> servicesList = new ArrayList<>();
        for (Map<String, Object> service : services.values()) {
            String serviceName = ((String) service.get("value"));
            service.put("name", serviceName);
            List<Map<String, Object>> entitiesByService = getEntitiesByService(service, apiModel);
            service.put("entities", entitiesByService);
            boolean isGenerateService = entitiesByService.stream().anyMatch(entity -> isGenerateEntity(entity));
            if (!isGenerateService) {
                continue;
            }
            servicesList.add(service);
            for (TemplateInput template : templates.serviceTemplates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("service", service, "entities", entitiesByService)));
            }
        }

        for (TemplateInput template : templates.allServicesTemplates) {
            templateOutputList.addAll(generateTemplateOutput(contextModel, template, Map.of("services", servicesList, "entities", new ArrayList(entities.values()))));
        }


        for (TemplateInput template : templates.singleTemplates) {
            templateOutputList.addAll(generateTemplateOutput(contextModel, template, Collections.emptyMap()));
        }

        return templateOutputList;
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
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put(sourceProperty, getZDLModel(contextModel));
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }
}
