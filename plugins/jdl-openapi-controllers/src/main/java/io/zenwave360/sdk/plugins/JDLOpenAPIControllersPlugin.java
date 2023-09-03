package io.zenwave360.sdk.plugins;

import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.EnrichOpenAPIWithJDLProcessor;
import io.zenwave360.sdk.processors.JDLDummyDataFromSchemasProcessor;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

/**
 * After you have generated SpringMVC interfaces and DTOs with OpenAPI generator, you can use this command to generate implementations (skeletons) and mappers for those interfaces and dtos:
 *
 * ```shell
 * jbang zw -p io.zenwave360.sdk.plugins.JDLOpenAPIControllersPlugin \
 *     specFile=src/main/resources/model/openapi.yml \
 *     jdlFile=src/main/resources/model/orders-model.jdl \
 *     basePackage=io.zenwave360.example \
 *     openApiApiPackage=io.zenwave360.example.adapters.web \
 *     openApiModelPackage=io.zenwave360.example.adapters.web.model \
 *     openApiModelNameSuffix=DTO \
 *     targetFolder=.
 * ```
 */
@DocumentedPlugin(value = "Generates implementations based on JDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.", shortCode = "jdl-openapi-controllers")
public class JDLOpenAPIControllersPlugin extends Plugin {

    @DocumentedOption(description = "JDL file to parse", required = false)
    public String jdlFile;

    public JDLOpenAPIControllersPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, JDLParser.class, JDLProcessor.class, EnrichOpenAPIWithJDLProcessor.class, JDLOpenAPIControllersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {

        if (!getOptions().containsKey("jdlFile")) {
            removeFromChain(JDLParser.class, JDLProcessor.class);
            addBeforeInChain(EnrichOpenAPIWithJDLProcessor.class, JDLDummyDataFromSchemasProcessor.class);
        }
        // because we have more than one model, we need to configure how they are passed around from parser to processor and generator
        // we use class name for passing the properties, in case one class is repeated in chain we'd use the index number in the chain
        withOption("DefaultYamlParser.specFile", StringUtils.firstNonBlank(this.getSpecFile(), (String) getOptions().get("openapiFile")));
        withOption("JDLParser.specFile", getOptions().get("jdlFile"));
        return (T) this;
    }
}
