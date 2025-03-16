package io.zenwave360.sdk.formatters;

import java.util.List;
import java.util.stream.Collectors;


import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;

public class PalantirJavaFormatter implements Formatter {

    private static Logger log = LoggerFactory.getLogger(PalantirJavaFormatter.class);
    private static boolean warnFixImports = true;

    @DocumentedOption(description = "Skip java sources output formatting")
    public boolean skipFormatting = false;

    @DocumentedOption(description = "Halt on formatting errors")
    public boolean haltOnFailFormatting = true;

    public PalantirJavaFormatter(boolean skipFormatting, boolean haltOnFailFormatting) {
        this.skipFormatting = skipFormatting;
        this.haltOnFailFormatting = haltOnFailFormatting;
    }

    private final com.palantir.javaformat.java.Formatter formatter =
            com.palantir.javaformat.java.Formatter.createFormatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

    public List<TemplateOutput> format(List<TemplateOutput> templateOutputList) {
        return templateOutputList.stream().map(t -> format(t)).collect(Collectors.toList());
    }

    public TemplateOutput format(TemplateOutput templateOutput) {
        if (skipFormatting) {
            log.debug("Skipping java formatting");
            return templateOutput;
        }
        if (templateOutput.getMimeType() != null && templateOutput.getMimeType().equals(OutputFormatType.JAVA.toString())) {
            if(templateOutput.getContent().startsWith("// formatter:off")) {
                log.debug("Skipping java formatting for file {}", templateOutput.getTargetFile());
                return templateOutput;
            }
            try {
                String formattedSource = templateOutput.getContent();
                formattedSource = formatter.formatSource(formattedSource);
                formattedSource = ImportsOrganizer.organizeImports(formattedSource);
                return new TemplateOutput(templateOutput.getTargetFile(), formattedSource, templateOutput.getMimeType(), templateOutput.isSkipOverwrite());
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
