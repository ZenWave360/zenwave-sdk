package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
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

@DocumentedPlugin(value = "Generates tests for Spring Cloud Streams Consumers.")
public class SpringCloudStreams3AdaptersPlugin extends Plugin {

    private Logger log = LoggerFactory.getLogger(getClass());
    public SpringCloudStreams3AdaptersPlugin() {
        super();
//        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, SpringCloudStreams3AdaptersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, ZDLParser.class, ZDLProcessor.class, SpringCloudStreams3AdaptersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {

        if (!getOptions().containsKey("zdlFile")) {
            removeFromChain(ZDLParser.class, ZDLProcessor.class);
        }
        withOption("DefaultYamlParser.specFile", StringUtils.firstNonBlank(this.getSpecFile(), (String) getOptions().get("apiFile")));
        withOption("ZDLParser.specFile", getOptions().get("zdlFile"));
        return (T) this;
    }
}
