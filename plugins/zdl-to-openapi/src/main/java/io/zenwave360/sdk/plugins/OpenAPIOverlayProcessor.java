package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.YamlOverlayGeneratedFilesProcessor;

import java.util.List;

public class OpenAPIOverlayProcessor extends YamlOverlayGeneratedFilesProcessor {

    @DocumentedOption(description = "OpenAPI file to be merged on top of the generated OpenAPI file")
    public String openapiMergeFile;

    @DocumentedOption(description = "Ordered list of overlay resources to apply on top of the generated OpenAPI file")
    public List<String> openapiOverlayFiles;

    @Override
    protected String getMergeFile() {
        return openapiMergeFile;
    }

    @Override
    protected List<String> getOverlayFiles() {
        return openapiOverlayFiles;
    }
}
