package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.*;
import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.writers.TemplateFileWriter;

/**
 * After you have generated SpringMVC interfaces and DTOs with OpenAPI generator, you can use this command to generate implementations (skeletons) and mappers for those interfaces and dtos:
 *
 * ```shell
 * jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin \
 *     specFile=src/main/resources/model/openapi.yml \
 *     zdlFile=src/main/resources/model/orders-model.zdl \
 *     basePackage=io.zenwave360.example \
 *     openApiApiPackage=io.zenwave360.example.adapters.web \
 *     openApiModelPackage=io.zenwave360.example.adapters.web.model \
 *     openApiModelNameSuffix=DTO \
 *     targetFolder=.
 * ```
 */
@DocumentedPlugin(value = "Generates implementations based on ZDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.", shortCode = "openapi-controllers")
public class OpenAPIControllersPlugin extends Plugin {

    @DocumentedOption(description = "ZDL file to parse", required = false)
    public String zdlFile;

    public OpenAPIControllersPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, ZDLParser.class, ZDLProcessor.class, OpenAPIControllersGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {

        if (!getOptions().containsKey("zdlFile")) {
            removeFromChain(ZDLParser.class, ZDLProcessor.class);
            addBeforeInChain(OpenAPIControllersGenerator.class, DummyDataFromSchemasProcessor.class);
            withOption("haltOnFailFormatting", false);
        }
        // because we have more than one model, we need to configure how they are passed around from parser to processor and generator
        // we use class name for passing the properties, in case one class is repeated in chain we'd use the index number in the chain
        withOption("DefaultYamlParser.specFile", StringUtils.firstNonBlank(this.getSpecFile(), (String) getOptions().get("openapiFile")));
        withOption("ZDLParser.specFile", getOptions().get("zdlFile"));
        return (T) this;
    }
}
