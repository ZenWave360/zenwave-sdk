package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates tests for Spring Cloud Streams Consumers.", shortCode = "spring-cloud-streams3-adapters")
public class SpringCloudStreams3AdaptersPlugin extends Plugin {

    private Logger log = LoggerFactory.getLogger(getClass());
    public SpringCloudStreams3AdaptersPlugin() {
        super();
//        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, SpringCloudStreams3AdaptersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, JDLParser.class, JDLProcessor.class, EnrichAsyncAPIWithJDLProcessor.class, SpringCloudStreams3AdaptersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {

        if (!getOptions().containsKey("jdlFile")) {
            removeFromChain(JDLParser.class, JDLProcessor.class);
//            addBeforeInChain(EnrichAsyncAPIWithJDLProcessor.class, JDLDummyDataFromSchemasProcessor.class);
//            withOption("JDLDummyDataFromSchemasProcessor.apiProperty", "api");
//            withOption("JDLDummyDataFromSchemasProcessor.jdlProperty", "jdl");
        }
        // because we have more than one model, we need to configure how they are passed around from parser to processor and generator
        // we use class name for passing the properties, in case one class is repeated in chain we'd use the index number in the chain
        withOption("DefaultYamlParser.specFile", StringUtils.firstNonBlank(this.getSpecFile(), (String) getOptions().get("apiFile")));
//        withOption("DefaultYamlParser.targetProperty", "api");
//        withOption("AsyncApiProcessor.targetProperty", "api");
        withOption("JDLParser.specFile", getOptions().get("jdlFile"));
//        withOption("JDLParser.targetProperty", "jdl");
//        withOption("JDLProcessor.targetProperty", "jdl");
//        withOption("EnrichAsyncAPIWithJDLProcessor.apiProperty", "api");
//        withOption("EnrichAsyncAPIWithJDLProcessor.jdlProperty", "jdl");
//        withOption("JDLOpenAPIControllersGenerator.openapiProperty", "api");
//        withOption("JDLOpenAPIControllersGenerator.jdlProperty", "jdl");
        return (T) this;
    }
}
