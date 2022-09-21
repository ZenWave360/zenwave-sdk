package io.zenwave360.generator.parsers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.zenwave360.generator.doc.DocumentedOption;

public class JDLParser implements Parser {

    @DocumentedOption(description = "JDL files to parse")
    public String[] specFiles;
    public String targetProperty = "jdl";

    public Map<String, String> options = new HashMap<>();

    @DocumentedOption(description = "JDL file to parse")
    public void setSpecFile(String specFile) {
        this.specFiles = new String[] {specFile};
    }

    public JDLParser withSpecFile(String... specFile) {
        this.specFiles = specFile;
        return this;
    }

    public JDLParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    public JDLParser withOptions(String option, String value) {
        this.options.put(option, value);
        return this;
    }

    protected String loadSpecFile(String specFile) {
        if (specFile.startsWith("classpath:")) {
            try {
                return new String(getClass().getClassLoader().getResourceAsStream(specFile.replaceFirst("classpath:", "")).readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return Files.readString(Paths.get(specFile), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        String jdlString = Arrays.stream(specFiles).map(file -> loadSpecFile(file)).collect(Collectors.joining());
        Map<String, Object> jdlModel = io.zenwave360.jhipster.jdl.JDLParser.parseJDL(jdlString);
        Map model = new LinkedHashMap<>();
        model.put(targetProperty, jdlModel);
        return model;
    }
}
