package io.zenwave360.sdk.formatters;



import com.google.googlejavaformat.java.CustomFormatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import io.spring.javaformat.config.JavaFormatConfig;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class SpringJavaFormatter implements Formatter {
    private static Logger log = LoggerFactory.getLogger(SpringJavaFormatter.class);

    @DocumentedOption(description = "Skip java sources output formatting")
    public boolean skipFormatting = false;

    @DocumentedOption(description = "Halt on formatting errors")
    public boolean haltOnFailFormatting = true;

    public SpringJavaFormatter(boolean skipFormatting, boolean haltOnFailFormatting) {
        this.skipFormatting = skipFormatting;
        this.haltOnFailFormatting = haltOnFailFormatting;
    }

    private final io.spring.javaformat.formatter.Formatter formatter =
            new io.spring.javaformat.formatter.Formatter(JavaFormatConfig.findFrom(new File(".")));

    private final CustomFormatter googleCustomFormatter = new CustomFormatter();


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
                String formattedSource = googleCustomFormatter.formatSourceAndFixImports(templateOutput.getContent()); // removes empty lines and formats imports
                formattedSource = formatSourceWithSpringJavaFormat(formattedSource); // re-formats the rest of the code
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
            } catch (BadLocationException e) {
                e.printStackTrace();
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

    protected String formatSourceWithSpringJavaFormat(String source) throws BadLocationException {
        var edit = formatter.format(source);
        IDocument document = new Document(source);
        edit.apply(document);
        return document.get();
    }

    public String fixImports(String input) throws FormatterException {
        input = ImportOrderer.reorderImports(input);
        input = RemoveUnusedImports.removeUnusedImports(input);
        return input;
    }
}
