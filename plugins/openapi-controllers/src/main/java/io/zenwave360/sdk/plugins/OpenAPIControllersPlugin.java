package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.*;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.writers.TemplateFileWriter;

/**
 * After you have generated SpringMVC interfaces and DTOs with OpenAPI generator, you can use this command to generate implementations (skeletons) and mappers for those interfaces and dtos:
 *
 * ```shell
 * jbang zw -p io.zenwave360.sdk.plugins.OpenAPIControllersPlugin \
 *     apiFile=src/main/resources/model/openapi.yml \
 *     zdlFile=src/main/resources/model/orders-model.zdl \
 *     basePackage=io.zenwave360.example \
 *     openApiApiPackage=io.zenwave360.example.adapters.web \
 *     openApiModelPackage=io.zenwave360.example.adapters.web.model \
 *     openApiModelNameSuffix=DTO \
 *     targetFolder=.
 * ```
 */
@DocumentedPlugin(summary = "Generates implementations based on ZDL models and OpenAPI definitions SpringMVC generated OpenAPI interfaces.")
public class OpenAPIControllersPlugin extends Plugin {

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
        return (T) this;
    }
}
