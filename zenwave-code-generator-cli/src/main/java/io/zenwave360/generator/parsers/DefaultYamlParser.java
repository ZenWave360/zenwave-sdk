package io.zenwave360.generator.parsers;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

public class DefaultYamlParser implements io.zenwave360.generator.parsers.Parser {

    @DocumentedOption(description = "API Specification File")
    public String specFile;
    public String targetProperty = "api";

    public DefaultYamlParser withSpecFile(String specFile) {
        this.specFile = specFile;
        return this;
    }

    public DefaultYamlParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    protected File findSpecFile(String specFile) {
        if(specFile.startsWith("classpath:")) {
            try {
                return new File(getClass().getClassLoader().getResource(specFile.replaceFirst("classpath:", "")).toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return new File(specFile);
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        File file = findSpecFile(specFile);
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular(SKIP));
        Map model = new LinkedHashMap<>();
        model.put(targetProperty, new Model(file, parser.parse().dereference().mergeAllOf().getRefs()));
        return model;
    }
}
