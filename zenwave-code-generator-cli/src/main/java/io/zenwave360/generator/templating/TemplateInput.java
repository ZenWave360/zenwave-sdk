package io.zenwave360.generator.templating;

import java.util.function.Supplier;

public class TemplateInput {
    private String templateLocation;
    private String targetFile;

    private String mimeType;
    private Supplier<Boolean> skip;

    public TemplateInput(String templateLocation, String targetFile) {
        this.templateLocation = templateLocation;
        this.targetFile = targetFile;
    }

    public TemplateInput(TemplateInput templateInput) {
        this.templateLocation = templateInput.templateLocation;
        this.targetFile = templateInput.targetFile;
        this.mimeType = templateInput.mimeType;
        this.skip = templateInput.skip;
    }

    public String getTemplateLocation() {
        return templateLocation;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public Supplier<Boolean> getSkip() {
        return skip;
    }

    public String getMimeType() {
        return mimeType;
    }

    public TemplateInput withTemplateLocation(String templateLocation) {
        this.templateLocation = templateLocation;
        return this;
    }

    public TemplateInput withTargetFile(String targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    public TemplateInput withMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public TemplateInput withSkip(Supplier<Boolean> skip) {
        this.skip = skip;
        return this;
    }
}
