package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.Formatter;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(
        title = "AsyncAPI and Spring Cloud Stream Generator with DTOs",
        summary =
                "Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI with Avro and JSON DTOs",
        mainOptions = {
            "apiFile",
            "apiFiles",
            "role",
            "style",
            "modelPackage",
            "producerApiPackage",
            "consumerApiPackage",
            "apiPackage",
            "transactionalOutbox",
            "bindingPrefix",
            "bindingSuffix",
            "generatedAnnotationClass"
        },
        hiddenOptions = {"layout", "zdlFile", "zdlFiles"})
public class SpringCloudStreamsWithDtosPlugin extends Plugin {

    public SpringCloudStreamsWithDtosPlugin() {
        super();
        withChain(
                DefaultYamlParser.class,
                AsyncApiProcessor.class,
                AsyncApiJsonSchema2PojoGenerator.class,
                AvroAsyncApiProcessor.class,
                SpringCloudStreams3Generator.class,
                JavaFormatter.class,
                TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("formatter")) {
            withOption("formatter", Formatter.Formatters.spring);
        }
        return super.processOptions();
    }
}
