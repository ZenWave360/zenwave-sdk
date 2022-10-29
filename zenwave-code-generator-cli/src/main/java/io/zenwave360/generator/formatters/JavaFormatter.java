package io.zenwave360.generator.formatters;

import java.util.List;
import java.util.stream.Collectors;

import com.google.googlejavaformat.java.CustomFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.googlejavaformat.java.FormatterException;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateOutput;

public class JavaFormatter implements Formatter {

    private static Logger log = LoggerFactory.getLogger(JavaFormatter.class);

    @DocumentedOption(description = "Skip java sources output formatting")
    public boolean skipFormatting = false;

    @DocumentedOption(description = "Halt on formatting errors")
    public boolean haltOnFailFormatting = true;

    public List<TemplateOutput> format(List<TemplateOutput> templateOutputList) {
        return templateOutputList.stream().map(t -> format(t)).collect(Collectors.toList());
    }

    public TemplateOutput format(TemplateOutput templateOutput) {
        if (skipFormatting) {
            log.debug("Skipping java formatting");
            return templateOutput;
        }
        if (templateOutput.getMimeType() != null && templateOutput.getMimeType().equals(OutputFormatType.JAVA.toString())) {
            try {
                String formattedSource = new CustomFormatter().formatSourceAndFixImports(templateOutput.getContent());
                return new TemplateOutput(templateOutput.getTargetFile(), formattedSource, templateOutput.getMimeType());
            } catch (FormatterException e) {
                if (e.diagnostics() != null && e.diagnostics().size() > 0) {
                    int line = e.diagnostics().get(0).line();
                    String lineText = getLine(templateOutput.getContent(), line + 1);
                    log.error("Formatting error at {}:{} -> \"{}\"", templateOutput.getTargetFile(), line, lineText, e);
                }
                if(haltOnFailFormatting) {
                    throw new RuntimeException(e);
                }
            }
        }
        return templateOutput;
    }

    public String getLine(String content, int line) {
        try {
            return content.split("\\r?\\n")[line];
        } catch (Exception e) {
            return "";
        }
    }
}
