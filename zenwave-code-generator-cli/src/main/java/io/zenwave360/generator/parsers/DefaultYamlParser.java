package io.zenwave360.generator.parsers;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;

public class DefaultYamlParser implements io.zenwave360.generator.parsers.Parser {

    @DocumentedOption(description = "API Specification File")
    public URI specFile;
    public String targetProperty = "api";

    private ClassLoader projectClassLoader;

    @Override
    public DefaultYamlParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public DefaultYamlParser withSpecFile(URI specFile) {
        this.specFile = specFile;
        return this;
    }

    public DefaultYamlParser withSpecFile(File specFile) {
        this.specFile = specFile.getAbsoluteFile().toURI();
        return this;
    }

    public DefaultYamlParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        $RefParser parser = new $RefParser(specFile)
                .withResourceClassLoader(this.projectClassLoader)
                .withOptions(new $RefParserOptions().withOnCircular(SKIP));
        Map model = new LinkedHashMap<>();
        model.put(targetProperty, new Model(specFile, parser.parse().dereference().mergeAllOf().getRefs()));
        return model;
    }
}
