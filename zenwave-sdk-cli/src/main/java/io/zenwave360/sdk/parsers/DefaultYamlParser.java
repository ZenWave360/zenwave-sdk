package io.zenwave360.sdk.parsers;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;
import io.zenwave360.jsonrefparser.$RefParserOptions.OnMissing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultYamlParser implements io.zenwave360.sdk.parsers.Parser {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "API Specification File")
    public URI apiFile;
    public String targetProperty = "api";

    private ClassLoader projectClassLoader;

    @DocumentedOption(description = "API Specification File (@deprecated use apiFile)")
    public void setSpecFile(URI specFile) {
        apiFile = specFile;
    }

    @DocumentedOption(description = "API Specification File (@deprecated use apiFile)")
    public void setOpenapiFile(URI openapiFile) {
        apiFile = openapiFile;
    }


    @Override
    public DefaultYamlParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public DefaultYamlParser withApiFile(URI specFile) {
        this.apiFile = specFile;
        return this;
    }

    public DefaultYamlParser withApiFile(File specFile) {
        this.apiFile = specFile.getAbsoluteFile().toURI();
        return this;
    }

    public DefaultYamlParser withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        Map<String, Object> model = new LinkedHashMap<>();
        if(apiFile != null) {
            $RefParser parser = new $RefParser(apiFile)
                    .withResourceClassLoader(this.projectClassLoader)
                    .withOptions(new $RefParserOptions().withOnCircular(SKIP).withOnMissing(OnMissing.SKIP));
            model.put(targetProperty, new Model(apiFile, parser.parse().dereference().mergeAllOf().getRefs()));
        } else {
            log.error("No API Specification (apiFile) provided");
        }
        return model;
    }
}
