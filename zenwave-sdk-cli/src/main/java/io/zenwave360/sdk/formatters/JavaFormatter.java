package io.zenwave360.sdk.formatters;

import java.util.List;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.TemplateOutput;

public class JavaFormatter implements Formatter {

    @DocumentedOption(description = "Code formatter implementation")
    public Formatters formatter = Formatters.spring;

    @DocumentedOption(description = "Skip java sources output formatting")
    public boolean skipFormatting = false;

    @DocumentedOption(description = "Halt on formatting errors")
    public boolean haltOnFailFormatting = true;
    private Formatter delegate = new SpringJavaFormatter(skipFormatting, haltOnFailFormatting);

    public void onPropertiesSet() {
        switch (formatter) {
            case google:
                delegate = new GoogleJavaFormatter(skipFormatting, haltOnFailFormatting);
                break;
            case palantir:
                delegate = new PalantirJavaFormatter(skipFormatting, haltOnFailFormatting);
                break;
            case spring:
                delegate = new SpringJavaFormatter(skipFormatting, haltOnFailFormatting);
                break;
            default:
                throw new RuntimeException("Unknown java formatter: " + formatter);
        }
    }

    @Override
    public List<TemplateOutput> format(List<TemplateOutput> templateOutputList) {
        return delegate.format(templateOutputList);
    }
}
