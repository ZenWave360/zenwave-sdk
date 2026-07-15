package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.writers.TemplateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class EventCatalogImportFileWriter implements TemplateWriter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Target folder for generated importer files.")
    public File targetFolder;

    @Override
    public void write(List<TemplateOutput> templateOutputList) {
        if (targetFolder == null) {
            throw new IllegalStateException("targetFolder must be set on EventCatalogImportFileWriter");
        }
        for (TemplateOutput output : templateOutputList) {
            writeFile(output.getTargetFile(), output.getContent());
        }
    }

    private void writeFile(String targetFile, String content) {
        try {
            File file = new File(targetFolder, targetFile);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            log.info("Writing {}", file.getAbsolutePath());
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot write " + targetFile, e);
        }
    }
}
