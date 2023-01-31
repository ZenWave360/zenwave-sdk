package io.zenwave360.sdk.templating;

public class TemplateOutput {

    private String targetFile;
    private String content;
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

    public String getTargetFile() {
        return targetFile;
    }

    public String getContent() {
        return content;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isSkipOverwrite() {
        return skipOverwrite;
    }
}
