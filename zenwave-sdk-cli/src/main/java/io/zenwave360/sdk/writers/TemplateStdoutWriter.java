package io.zenwave360.sdk.writers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.CliOutput;

public class TemplateStdoutWriter implements TemplateWriter {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void write(List<TemplateOutput> templateOutputList) {
        templateOutputList.stream()
                .peek(t -> log.debug("Writting template to file: {}", t.getTargetFile()))
                .forEach(t -> write(t.getTargetFile(), t.getContent()));
    }

    protected void write(String file, String contents) {
        log.debug("Writting template output to file: {}", file);
        CliOutput.stdout().println("------- " + file + " ------");
        CliOutput.stdout().println(contents);
        CliOutput.stdout().println("--- end: " + file + " -----\n");
    }
}
