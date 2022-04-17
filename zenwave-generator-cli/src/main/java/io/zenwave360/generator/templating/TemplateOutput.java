package io.zenwave360.generator.templating;

import java.io.File;

public class TemplateOutput {

    private String targetFile;
    private String content;

    public TemplateOutput(String targetFile, String content) {
        this.targetFile = targetFile;
        this.content = content;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getContent() {
        return content;
    }
}
