package io.zenwave360.generator.templating;

public class TemplateOutput {

    private String targetFile;
    private String content;

    private String mimeType;

    public TemplateOutput(String targetFile, String content) {
        this.targetFile = targetFile;
        this.content = content;
    }

    public TemplateOutput(String targetFile, String content, String mimeType) {
        this.targetFile = targetFile;
        this.content = content;
        this.mimeType = mimeType;
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
}
