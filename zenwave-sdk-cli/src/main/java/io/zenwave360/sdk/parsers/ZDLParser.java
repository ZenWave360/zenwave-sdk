package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.github.zenwave360.zdl.ZdlParser;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.plugins.ConfigurationProvider;
import io.zenwave360.sdk.utils.JSONPath;

public class ZDLParser implements Parser, ConfigurationProvider {

    public static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob", "byte");

    @DocumentedOption(description = "ZDL files to parse")
    public List<String> zdlFiles = List.of();
    private String content;
    public String targetProperty = "zdl";

    @DocumentedOption(description = "Continue even when ZDL contains fatal errors")
    public boolean continueOnZdlError = true;

    public Map<String, String> options = new HashMap<>();

    private ClassLoader projectClassLoader;

    @DocumentedOption(description = "ZDL file to parse (@deprecated use zdlFile)")
    public void setSpecFile(String specFile) {
        setZdlFile(specFile);
    }

    @DocumentedOption(description = "ZDL files to parse (@deprecated use zdlFiles)")
    public void setSpecFiles(List<String> specFiles) {
        setZdlFiles(specFiles);
    }

    @DocumentedOption(description = "ZDL file to parse")
    public void setZdlFile(String zdlFile) {
        if(zdlFile != null) {
            this.zdlFiles = List.of(zdlFile);
        }
    }

    @DocumentedOption(description = "ZDL file to parse")
    public void setZdlFiles(List<String> zdlFiles) {
        this.zdlFiles = zdlFiles;
    }

    public ZDLParser withContent(String content) {
        this.content = content;
        return this;
    }

    public ZDLParser withZdlFile(String zdlFile) {
        this.zdlFiles = List.of(zdlFile);
        return this;
    }

    public ZDLParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    public ZDLParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        String zdlString = content;
        if(zdlString == null) {
            zdlString = zdlFiles.stream().map(this::loadSpecFile).collect(Collectors.joining());
        }
        Map<String, Object> zdlModel = new ZdlParser().parseModel(zdlString);
        var problems = JSONPath.get(zdlModel, "$.problems", List.of());
        if(!problems.isEmpty()) {
            for (Object problem : problems) {
                var message = JSONPath.get(problem, "message");
                var location = JSONPath.get(problem, "location", new int[5]);
                var path = JSONPath.get(problem, "path");
                System.err.printf("ZDL ERROR [%s]: %s [line: %s, char: %s]%n", path, message, location[2], location[3]+1);
            }
            if(!continueOnZdlError) {
                throw new ParseProblemsException(problems);
            }
        }
        Map<String, Object> model = new LinkedHashMap<>();
        model.put(targetProperty, zdlModel);
        return model;
    }

    @Override
    public void updateConfiguration(Plugin configuration, Map<String, Object> model) {
        var zdl = model.get(targetProperty);
        var config = JSONPath.get(zdl, "$.config", Map.<String, Object>of());

        for (var entry : config.entrySet()) {
            if(!configuration.getOptions().containsKey(entry.getKey())) {
                configuration.withOption(entry.getKey(), entry.getValue());
            }
        }
        if(config.containsKey("layout")) {
            configuration.withLayout((String) config.get("layout"));
            configuration.processLayout();
        }
    }
}
