package io.zenwave360.sdk.templating;

import java.util.Map;

public class TemplateOutput {

    private TemplateInput templateInput;
    private String targetFile;
    private String content;
    private Map<String, Object> context;
    private String mimeType;
    private boolean skipOverwrite = false;

    public TemplateOutput(String targetFile, String content) {
        this.targetFile = targetFile;
        this.content = content;
    }

    public TemplateOutput(String targetFile, String content, String mimeType) {
        this.targetFile = targetFile;
        this.content = content;
        this.mimeType = mimeType;
    }

    public TemplateOutput(String targetFile, String content, String mimeType, boolean skipOverwrite) {
        this.targetFile = targetFile;
        this.content = content;
        this.mimeType = mimeType;
        this.skipOverwrite = skipOverwrite;
    }

    public TemplateOutput(TemplateInput templateInput, String targetFile, Map<String, Object> context, String mimeType, boolean skipOverwrite) {
        this.templateInput = templateInput;
        this.targetFile = targetFile;
        this.context = context;
        this.mimeType = mimeType;
        this.skipOverwrite = skipOverwrite;
    }

    public TemplateInput getTemplateInput() {
        return templateInput;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isSkipOverwrite() {
        return skipOverwrite;
    }
}
