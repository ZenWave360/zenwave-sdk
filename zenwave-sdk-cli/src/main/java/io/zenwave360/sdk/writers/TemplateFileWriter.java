package io.zenwave360.sdk.writers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.TemplateOutput;

public class TemplateFileWriter implements TemplateWriter {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Target folder to generate code to. If left empty, it will print to stdout.")
    private File targetFolder;

    public TemplateFileWriter withTargetFolder(File targetFolder) {
        this.targetFolder = targetFolder;
        return this;
    }

    public void setTargetFolder(File targetFolder) {
        this.targetFolder = targetFolder;
    }

    @Override
    public void write(List<TemplateOutput> templateOutputList) {
        templateOutputList.stream()
                .peek(t -> log.info("Writing template with targetFile: {}", t.getTargetFile()))
                .forEach(t -> writeToFile(getFile(t.getTargetFile()), t.getContent(), t.isSkipOverwrite()));
    }

    protected File getFile(String fileName) {
        return targetFolder != null ? new File(targetFolder, fileName) : new File(fileName);
    }

    protected void writeToFile(File file, String contents, boolean skipOverwrite) {
        log.trace("Writing template output to file: {}", file);
        try {
            file.getParentFile().mkdirs();
            if(skipOverwrite && Files.exists(Paths.get(file.toURI()))) {
                log.warn("Skipping overwriting file: {}", file);
                return;
            }
            Files.writeString(Paths.get(file.toURI()),
                    contents,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
