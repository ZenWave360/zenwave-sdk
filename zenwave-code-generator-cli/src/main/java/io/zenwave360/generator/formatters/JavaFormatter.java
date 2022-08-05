package io.zenwave360.generator.formatters;

import com.google.googlejavaformat.java.FormatterException;
import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class JavaFormatter implements Formatter {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Skip java sources output formatting")
    public boolean skipFormatting = false;

    public List<TemplateOutput> format(List<TemplateOutput> templateOutputList) {
        return templateOutputList.stream().map(t -> format(t)).collect(Collectors.toList());
    }


    public TemplateOutput format(TemplateOutput templateOutput) {
        if(skipFormatting) {
            log.debug("Skipping java formatting");
            return templateOutput;
        }
        if(templateOutput.getMimeType() != null && templateOutput.getMimeType().equals(OutputFormatType.JAVA.toString())) {
            try {
                String formattedSource = new com.google.googlejavaformat.java.Formatter().formatSourceAndFixImports(templateOutput.getContent());
                return new TemplateOutput(templateOutput.getTargetFile(), formattedSource, templateOutput.getMimeType());
            } catch (FormatterException e) {
                if(e.diagnostics() != null && e.diagnostics().size() > 0) {
                    int line = e.diagnostics().get(0).line();
                    String lineText = getLine(templateOutput.getContent(), line + 1);
                    log.error("Formatting error at {}:{} -> \"{}\"", templateOutput.getTargetFile(), line, lineText, e);
                }
                throw new RuntimeException(e);
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
