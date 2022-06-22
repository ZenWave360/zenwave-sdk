package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class JDLOpenAPIControllersConfigurationPreset extends Configuration {

    public static final String PRESET_ID = "jdl-entities";

    public JDLOpenAPIControllersConfigurationPreset() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, JDLParser.class, JDLProcessor.class, JDLOpenAPIControllersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Configuration> T processOptions() {
        if(!getOptions().containsKey("targetFolder")) {
            withChain(DefaultYamlParser.class, OpenApiProcessor.class, JDLParser.class, JDLProcessor.class, JDLOpenAPIControllersGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        // because we have more than one model, we need to configure how they are passed around from parser to processor and generator
        withOption("0.specFile", StringUtils.firstNonBlank(this.getSpecFile(), (String) getOptions().get("openapiFile")));
        withOption("0.targetProperty", "openapi");
        withOption("1.targetProperty", "openapi");
        withOption("2.specFile", getOptions().get("jdlFile"));
        withOption("2.targetProperty", "jdl");
        withOption("3.targetProperty", "jdl");
        withOption("4.openapiProperty", "openapi");
        withOption("4.jdlProperty", "jdl");
        return (T) this;
    }
}
