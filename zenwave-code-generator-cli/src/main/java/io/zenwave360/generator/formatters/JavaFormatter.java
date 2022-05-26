package io.zenwave360.generator.formatters;

import com.google.googlejavaformat.java.FormatterException;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;
import java.util.stream.Collectors;

public class JavaFormatter implements Formatter {

    public List<TemplateOutput> format(List<TemplateOutput> templateOutputList) {
        return templateOutputList.stream().map(t -> format(t)).collect(Collectors.toList());
    }


    public TemplateOutput format(TemplateOutput templateOutput) {
        if(templateOutput.getMimeType() != null && templateOutput.getMimeType().contains("java")) {
            try {
                String formattedSource = new com.google.googlejavaformat.java.Formatter().formatSourceAndFixImports(templateOutput.getContent());
                return new TemplateOutput(templateOutput.getTargetFile(), formattedSource, templateOutput.getMimeType());
            } catch (FormatterException e) {
                throw new RuntimeException(e);
            }
        }
        return templateOutput;
    }
}
