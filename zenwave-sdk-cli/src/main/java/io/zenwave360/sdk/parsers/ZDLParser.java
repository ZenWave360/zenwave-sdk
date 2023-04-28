package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.github.zenwave360.zdl.ZdlParser;
import io.zenwave360.sdk.doc.DocumentedOption;

public class ZDLParser implements Parser {

    public static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob");

    @DocumentedOption(description = "ZDL files to parse")
    public String[] specFiles;
    public String targetProperty = "jdl";

    public Map<String, String> options = new HashMap<>();

    private ClassLoader projectClassLoader;

    @DocumentedOption(description = "ZDL file to parse")
    public void setSpecFile(String specFile) {
        this.specFiles = new String[] {specFile};
    }

    public ZDLParser withSpecFile(String... specFile) {
        this.specFiles = specFile;
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
        String zdlString = Arrays.stream(specFiles).map(this::loadSpecFile).collect(Collectors.joining());
        Map<String, Object> zdlModel = ZdlParser.parseModel(zdlString);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put(targetProperty, zdlModel);
        return model;
    }
}
