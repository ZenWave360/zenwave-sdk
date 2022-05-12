package io.zenwave360.generator.writers;

import io.zenwave360.generator.templating.TemplateOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class DefaultTemplateWriter implements TemplateWriter {

    private Logger log = LoggerFactory.getLogger(getClass());

    private File targetFolder;

    public void setTargetFolder(File targetFolder) {
        this.targetFolder = targetFolder;
    }

    @Override
    public void write(List<TemplateOutput> templateOutputList) {
        templateOutputList.stream()
                .peek(t -> log.debug("Writting template to file: {}", t.getTargetFile()))
                .forEach(t -> writeToFile(getFile(t.getTargetFile()), t.getContent()));
    }

    protected File getFile(String fileName) {
        return targetFolder != null? new File(targetFolder, fileName) : new File(fileName);
    }

    protected void writeToFile(File file, String contents) {
        log.debug("Writting template output to file: {}", file);
        try {
            file.getParentFile().mkdirs();
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
