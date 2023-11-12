package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.github.zenwave360.zdl.ZdlParser;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.utils.JSONPath;

public class ZDLParser implements Parser {

    public static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob");

    @DocumentedOption(description = "ZDL files to parse")
    public String[] specFiles;
    private String content;
    public String targetProperty = "zdl";

    @DocumentedOption(description = "Continue even when ZDL contains fatal errors")
    public boolean continueOnZdlError = true;

    public Map<String, String> options = new HashMap<>();

    private ClassLoader projectClassLoader;

    @DocumentedOption(description = "ZDL file to parse")
    public void setSpecFile(String specFile) {
        if(specFile == null) {
            this.specFiles = new String[] {};
        } else {
            this.specFiles = new String[] {specFile};
        }
    }

    public ZDLParser withSpecFile(String... specFile) {
        this.specFiles = specFile;
        return this;
    }

    public ZDLParser withContent(String content) {
        this.content = content;
        return this;
    }

    public ZDLParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    public ZDLParser withOptions(String option, String value) {
        this.options.put(option, value);
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
            zdlString = Arrays.stream(specFiles).map(this::loadSpecFile).collect(Collectors.joining());
        }
        Map<String, Object> zdlModel = ZdlParser.parseModel(zdlString);
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
}
