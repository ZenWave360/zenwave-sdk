package io.zenwave360.sdk.templating;

import java.util.Map;
import java.util.function.Function;

public class TemplateInput {
    private String templateLocation;
    private String targetFile;

    private String mimeType;
    private Function<Map<String, Object>, Boolean> skip;
    private boolean skipOverwrite = false;

    public TemplateInput() {}

    public TemplateInput(String templateLocation, String targetFile) {
        this.templateLocation = templateLocation;
        this.targetFile = targetFile;
    }

    public TemplateInput(String templateLocation, String targetFile, OutputFormatType mimeType) {
        this.templateLocation = templateLocation;
        this.targetFile = targetFile;
        withMimeType(mimeType);
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

    public Function<Map<String, Object>, Boolean> getSkip() {
        return skip;
    }

    public boolean isSkipOverwrite() {
        return skipOverwrite;
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

    public TemplateInput withMimeType(OutputFormatType mimeType) {
        this.mimeType = mimeType != null ? mimeType.toString() : null;
        return this;
    }

    public TemplateInput withMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public TemplateInput withSkip(Function<Map<String, Object>, Boolean> skip) {
        this.skip = skip;
        return this;
    }

    public TemplateInput withSkipOverwrite(boolean overwrite) {
        this.skipOverwrite = overwrite;
        return this;
    }
}
