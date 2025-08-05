package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.utils.JSONPath;

public class SpringCloudStreamsWithDtosGenerator extends Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder;

    @DocumentedOption(description = "Source folder inside folder to generate code to.")
    public String sourceFolder = "src/main/java";


    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        return null;
    }
}
