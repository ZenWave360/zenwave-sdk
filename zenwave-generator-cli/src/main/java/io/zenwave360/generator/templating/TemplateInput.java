package io.zenwave360.generator.templating;

import java.util.function.Supplier;

public class TemplateInput {
    private String templateLocation;
    private String targetFile;
    private Supplier<Boolean> skip;

    public TemplateInput(String templateLocation, String targetFile) {
        this.templateLocation = templateLocation;
        this.targetFile = targetFile;
    }

    public TemplateInput(String templateLocation, String targetFile, Supplier<Boolean> skip) {
        this.templateLocation = templateLocation;
        this.targetFile = targetFile;
        this.skip = skip;
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
}
