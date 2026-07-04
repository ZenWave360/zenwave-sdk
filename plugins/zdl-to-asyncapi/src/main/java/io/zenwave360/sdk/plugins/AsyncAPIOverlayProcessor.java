package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.YamlOverlayGeneratedFilesProcessor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class AsyncAPIOverlayProcessor extends YamlOverlayGeneratedFilesProcessor {

    @DocumentedOption(description = "AsyncAPI file to be merged on top of the generated AsyncAPI file")
    public String asyncapiMergeFile;

    @DocumentedOption(description = "Ordered list of overlay resources to apply on top of the generated AsyncAPI file")
    public List<String> asyncapiOverlayFiles;

    @Override
    protected String getMergeFile() {
        return asyncapiMergeFile;
    }

    @Override
    protected List<String> getOverlayFiles() {
        return asyncapiOverlayFiles;
    }

    @Override
    protected UnaryOperator<Map<String, Object>> getDocumentOrderer() {
        return AsyncAPIOverlayProcessor::orderAsyncAPIRootElements;
    }

    static Map<String, Object> orderAsyncAPIRootElements(Map<String, Object> document) {
        if (!document.containsKey("servers")) {
            return document;
        }

        Map<String, Object> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if ("servers".equals(entry.getKey())) {
                continue;
            }
            ordered.put(entry.getKey(), entry.getValue());
            if ("info".equals(entry.getKey())) {
                ordered.put("servers", document.get("servers"));
            }
        }
        if (!ordered.containsKey("servers")) {
            ordered.put("servers", document.get("servers"));
        }
        return ordered;
    }
}
