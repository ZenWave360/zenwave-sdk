package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GeneratedProjectFiles {

    public TemplateOutputListMap aggregates = new TemplateOutputListMap();

    public TemplateOutputListMap domainEvents = new TemplateOutputListMap();
    public TemplateOutputListMap entities = new TemplateOutputListMap();

    public TemplateOutputListMap enums = new TemplateOutputListMap();
    public TemplateOutputListMap inputs = new TemplateOutputListMap();

    public TemplateOutputListMap inputEnums = new TemplateOutputListMap();
    public TemplateOutputListMap eventEnums = new TemplateOutputListMap();
    public TemplateOutputListMap outputs = new TemplateOutputListMap();
    public TemplateOutputListMap services = new TemplateOutputListMap();
    public TemplateOutputListMap events = new TemplateOutputListMap();

    public List<TemplateOutput> allEntities = new ArrayList<>();
    public List<TemplateOutput> allDomainEvents = new ArrayList<>();
    public List<TemplateOutput> allEnums = new ArrayList<>();
    public List<TemplateOutput> allInputs = new ArrayList<>();
    public List<TemplateOutput> allOutputs = new ArrayList<>();
    public List<TemplateOutput> allServices = new ArrayList<>();
    public List<TemplateOutput> allExternalEvents = new ArrayList<>();
    public List<TemplateOutput> singleFiles = new ArrayList<>();

    public List<TemplateOutput> getAllTemplateOutputs() {
        var templateOutputList = new ArrayList<TemplateOutput>();
        aggregates.values().forEach(templateOutputList::addAll);
        domainEvents.values().forEach(templateOutputList::addAll);
        entities.values().forEach(templateOutputList::addAll);
        enums.values().forEach(templateOutputList::addAll);
        inputs.values().forEach(templateOutputList::addAll);
        inputEnums.values().forEach(templateOutputList::addAll);
        eventEnums.values().forEach(templateOutputList::addAll);
        outputs.values().forEach(templateOutputList::addAll);
        services.values().forEach(templateOutputList::addAll);
        events.values().forEach(templateOutputList::addAll);
        templateOutputList.addAll(allEntities);
        templateOutputList.addAll(allDomainEvents);
        templateOutputList.addAll(allEnums);
        templateOutputList.addAll(allInputs);
        templateOutputList.addAll(allOutputs);
        templateOutputList.addAll(allServices);
        templateOutputList.addAll(allExternalEvents);
        templateOutputList.addAll(singleFiles);
        return templateOutputList;
    }

    public static class TemplateOutputListMap extends LinkedHashMap<String, List<TemplateOutput>> {
        public void addAll(String key, List<TemplateOutput> templateOutput) {
            if(!containsKey(key)) {
                put(key, new ArrayList<>());
            }
            get(key).addAll(templateOutput);
        }
    }
}
