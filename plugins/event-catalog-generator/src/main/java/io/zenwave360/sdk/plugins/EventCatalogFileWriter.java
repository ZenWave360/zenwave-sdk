package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.writers.TemplateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * EventCatalog-aware file writer that adds two behaviours on top of plain file writing:
 *
 * <ol>
 *   <li><b>Service-page versioning</b> — before overwriting a service {@code index.mdx},
 *       reads the existing frontmatter {@code version} field. If the new version differs,
 *       the current file is moved to {@code versioned/{old-version}/index.mdx} inside the
 *       same service folder.</li>
 *   <li><b>Output replacement with versioned/ preservation</b> — the entire output folder
 *       is cleaned before writing new files, but any {@code versioned/} sub-directory is
 *       left untouched.</li>
 * </ol>
 *
 * <p>Write sequence:
 * <ol>
 *   <li>Archive any service {@code index.mdx} whose version has changed.</li>
 *   <li>Delete all files/directories in {@code targetFolder} except {@code versioned/} trees.</li>
 *   <li>Write all generated files.</li>
 * </ol>
 */
public class EventCatalogFileWriter implements TemplateWriter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder;

    public void setTargetFolder(File targetFolder) {
        this.targetFolder = targetFolder;
    }

    @Override
    public void write(List<TemplateOutput> templateOutputList) {
        if (targetFolder == null) {
            throw new IllegalStateException("targetFolder must be set on EventCatalogFileWriter");
        }

        // Step 1: archive service pages whose version has changed
        for (TemplateOutput output : templateOutputList) {
            if (isServiceIndexMdx(output.getTargetFile())) {
                maybeArchiveServicePage(output);
            }
        }

        // Step 2: clean output folder, preserving versioned/ trees
        cleanOutputFolder();

        // Step 3: write all generated files
        for (TemplateOutput output : templateOutputList) {
            writeFile(output.getTargetFile(), output.getContent());
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: service-page versioning
    // -------------------------------------------------------------------------

    /**
     * Returns true when the path matches the pattern for a service index page:
     * {@code domains/{d}/{sd}/services/{svc}/index.mdx} — exactly depth 6 relative
     * to the output root, with {@code services} as the 4th segment.
     */
    private boolean isServiceIndexMdx(String targetFile) {
        // Normalise separators so the check works on all platforms
        String normalised = targetFile.replace('\\', '/');
        String[] parts = normalised.split("/");
        // Expected: domains / domainId / subdomainId / services / serviceId / index.mdx
        return parts.length == 6
                && "domains".equals(parts[0])
                && "services".equals(parts[3])
                && "index.mdx".equals(parts[5]);
    }

    private void maybeArchiveServicePage(TemplateOutput output) {
        File existing = new File(targetFolder, output.getTargetFile());
        if (!existing.exists()) return;

        String existingVersion = readFrontmatterVersion(existing);
        String newVersion = extractVersionFromContent(output.getContent());

        if (existingVersion == null || existingVersion.equals(newVersion)) return;

        // Versions differ — archive current page
        File versionedDir = new File(existing.getParentFile(), "versioned/" + existingVersion);
        versionedDir.mkdirs();
        File archiveTarget = new File(versionedDir, "index.mdx");
        try {
            Files.move(existing.toPath(), archiveTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Archived service page version {} to {}", existingVersion, archiveTarget.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not archive service page {}: {}", existing.getAbsolutePath(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String readFrontmatterVersion(File file) {
        try {
            String content = Files.readString(file.toPath());
            String yaml = extractFrontmatterYaml(content);
            if (yaml == null) return null;
            Map<String, Object> fm = yamlMapper.readValue(yaml, Map.class);
            Object version = fm.get("version");
            return version != null ? version.toString() : null;
        } catch (Exception e) {
            log.warn("Could not read frontmatter version from {}: {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private String extractVersionFromContent(String content) {
        String yaml = extractFrontmatterYaml(content);
        if (yaml == null) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fm = yamlMapper.readValue(yaml, Map.class);
            Object version = fm.get("version");
            return version != null ? version.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Extracts the YAML content between the opening and closing {@code ---} delimiters. */
    private String extractFrontmatterYaml(String content) {
        if (content == null || !content.startsWith("---")) return null;
        int end = content.indexOf("\n---", 3);
        if (end < 0) return null;
        return content.substring(4, end).trim();
    }

    // -------------------------------------------------------------------------
    // Step 2: clean output folder
    // -------------------------------------------------------------------------

    /**
     * Deletes all files and directories under {@code targetFolder} except those
     * that are inside a {@code versioned/} sub-tree.
     */
    private void cleanOutputFolder() {
        if (!targetFolder.exists()) return;

        try {
            Files.walkFileTree(targetFolder.toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip the root itself
                    if (dir.equals(targetFolder.toPath())) return FileVisitResult.CONTINUE;
                    // Preserve entire versioned/ sub-trees
                    if ("versioned".equals(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // Don't delete the root folder itself
                    if (dir.equals(targetFolder.toPath())) return FileVisitResult.CONTINUE;
                    // Delete now-empty directories (versioned/ skipped above, so safe)
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException ignored) {
                        // Directory still has versioned/ content — leave it
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Could not fully clean output folder {}: {}", targetFolder.getAbsolutePath(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Step 3: write files
    // -------------------------------------------------------------------------

    private void writeFile(String targetFile, String content) {
        File file = new File(targetFolder, targetFile);
        try {
            file.getParentFile().mkdirs();
            log.info("Writing {}", targetFile);
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file.getAbsolutePath(), e);
        }
    }
}
