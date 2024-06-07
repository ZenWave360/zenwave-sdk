package io.zenwave360.sdk.formatters;

import com.facebook.ktfmt.format.Formatter;
import com.facebook.ktfmt.format.FormattingOptions;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class KotlinFormatter implements io.zenwave360.sdk.formatters.Formatter {

    private static Logger log = LoggerFactory.getLogger(KotlinFormatter.class);

    @DocumentedOption(description = "Skip kotlin sources output formatting")
    public boolean skipFormatting = false;

    @DocumentedOption(description = "Halt on formatting errors")
    public boolean haltOnFailFormatting = true;

    private final FormattingOptions formattingOptions = new FormattingOptions(120, 4, 4, true, true, false);

    public void format(GeneratedProjectFiles generatedProjectFiles) {
        generatedProjectFiles.getAllTemplateOutputs().stream().map(t -> format(t)).collect(Collectors.toList());
    }

    public TemplateOutput format(TemplateOutput templateOutput) {
        if (skipFormatting) {
            log.debug("Skipping kotlin formatting");
            return templateOutput;
        }
        if (templateOutput.getMimeType() != null && templateOutput.getMimeType().equals(OutputFormatType.KOTLIN.toString())) {
            if (templateOutput.getContent().startsWith("// formatter:off")) {
                log.debug("Skipping kotlin formatting for file {}", templateOutput.getTargetFile());
                return templateOutput;
            }
            try {
                String formattedSource = templateOutput.getContent();
                formattedSource = Formatter.format(formattingOptions, formattedSource);
                return new TemplateOutput(templateOutput.getTargetFile(), formattedSource, templateOutput.getMimeType(), templateOutput.isSkipOverwrite());
            } catch (Exception e) {
                log.error("Formatting error for file {}", templateOutput.getTargetFile(), e);
                if (haltOnFailFormatting) {
                    throw new RuntimeException(e);
                }
            }
        }
        return templateOutput;
    }
}
