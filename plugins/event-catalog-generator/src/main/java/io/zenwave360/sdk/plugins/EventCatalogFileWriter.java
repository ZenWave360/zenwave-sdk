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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EventCatalog-aware file writer that preserves historical versions while replacing
 * generated output.
 *
 * <p>Every supported EventCatalog resource follows the same layout:
 * {@code {collection}/{resource}/index.mdx} is current and
 * {@code {collection}/{resource}/versioned/{version}/index.mdx} is historical.
 * Before replacing the output, this writer archives a resource when its frontmatter
 * version changes or when it is no longer generated. The complete local resource
 * content is copied into the archive; nested resource collections and existing
 * {@code versioned/} history remain independent.</p>
 *
 * <p>Write sequence:</p>
 * <ol>
 *   <li>Archive changed and removed resources.</li>
 *   <li>Delete generated output while preserving every {@code versioned/} tree.</li>
 *   <li>Write the newly generated files.</li>
 * </ol>
 */
public class EventCatalogFileWriter implements TemplateWriter {

    /** Collections supported by EventCatalog's {@code versioned/{version}} layout. */
    private static final Set<String> VERSIONED_COLLECTIONS = Set.of(
            "domains", "subdomains", "systems", "services", "agents", "adrs",
            "events", "commands", "queries", "channels", "flows", "containers",
            "entities", "data-products", "diagrams");

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

        archiveChangedAndRemovedResources(templateOutputList);
        cleanOutputFolder();

        for (TemplateOutput output : templateOutputList) {
            writeFile(output.getTargetFile(), output.getContent());
        }
    }

    // -------------------------------------------------------------------------
    // Resource versioning
    // -------------------------------------------------------------------------

    private void archiveChangedAndRemovedResources(List<TemplateOutput> outputs) {
        if (!targetFolder.exists()) return;

        Map<Path, String> generatedVersions = generatedResourceVersions(outputs);
        Map<Path, Path> existingIndexes = findExistingResourceIndexes();

        // Archive children before their parents. Parent archives intentionally exclude
        // nested resource collections, but the ordering also makes that separation clear.
        List<Map.Entry<Path, Path>> resources = new ArrayList<>(existingIndexes.entrySet());
        resources.sort(Map.Entry.<Path, Path>comparingByKey(
                Comparator.comparingInt(Path::getNameCount).reversed()));

        for (Map.Entry<Path, Path> resource : resources) {
            String existingVersion = readFrontmatterVersion(resource.getValue().toFile());
            if (existingVersion == null || existingVersion.isBlank()) continue;

            String generatedVersion = generatedVersions.get(resource.getKey());
            boolean removed = generatedVersion == null;
            boolean changed = !removed && !existingVersion.equals(generatedVersion);
            if (removed || changed) {
                archiveResource(resource.getKey(), existingVersion);
            }
        }
    }

    private Map<Path, String> generatedResourceVersions(List<TemplateOutput> outputs) {
        Map<Path, String> versions = new HashMap<>();
        for (TemplateOutput output : outputs) {
            Path relativePath = relativeOutputPath(output.getTargetFile());
            if (relativePath == null || !isCurrentResourceIndex(relativePath)) continue;

            String version = extractVersionFromContent(output.getContent());
            if (version != null && !version.isBlank()) {
                versions.put(relativePath.getParent(), version);
            }
        }
        return versions;
    }

    private Map<Path, Path> findExistingResourceIndexes() {
        Path root = targetFolder.toPath().toAbsolutePath().normalize();
        Map<Path, Path> indexes = new LinkedHashMap<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(root) && "versioned".equals(fileName(dir))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relativePath = root.relativize(file);
                    if (isCurrentResourceIndex(relativePath)) {
                        indexes.merge(relativePath.getParent(), file,
                                (existing, candidate) -> "index.mdx".equals(fileName(candidate)) ? candidate : existing);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Cannot inspect EventCatalog resources in " + root, e);
        }
        return indexes;
    }

    private Path relativeOutputPath(String targetFile) {
        if (targetFile == null || targetFile.isBlank()) return null;
        Path path = Path.of(targetFile.replace('\\', '/')).normalize();
        if (path.isAbsolute()) return null;
        for (Path segment : path) {
            if ("..".equals(segment.toString())) return null;
        }
        return path;
    }

    private boolean isCurrentResourceIndex(Path relativePath) {
        if (relativePath == null || relativePath.getNameCount() < 3) return false;
        String filename = fileName(relativePath);
        if (!"index.md".equals(filename) && !"index.mdx".equals(filename)) return false;
        for (Path segment : relativePath) {
            if ("versioned".equals(segment.toString())) return false;
        }
        String collection = relativePath.getName(relativePath.getNameCount() - 3).toString();
        return VERSIONED_COLLECTIONS.contains(collection);
    }

    private void archiveResource(Path relativeResourceDir, String version) {
        if (!isSafeVersionDirectory(version)) {
            throw new IllegalArgumentException("Cannot archive EventCatalog resource " + relativeResourceDir
                    + " with unsafe version '" + version + "'");
        }

        Path resourceDir = targetFolder.toPath().toAbsolutePath().normalize().resolve(relativeResourceDir).normalize();
        Path versionedDir = resourceDir.resolve("versioned");
        Path archiveDir = versionedDir.resolve(version).normalize();
        if (!archiveDir.getParent().equals(versionedDir)) {
            throw new IllegalArgumentException(
                    "Cannot archive EventCatalog resource outside its versioned directory: " + relativeResourceDir);
        }

        try {
            deleteRecursively(archiveDir);
            Files.createDirectories(archiveDir);
            try (var entries = Files.list(resourceDir)) {
                for (Path entry : entries.toList()) {
                    String name = fileName(entry);
                    if ("versioned".equals(name) || isNestedResourceCollection(entry)) continue;
                    copyRecursively(entry, archiveDir.resolve(name));
                }
            }
            log.info("Archived EventCatalog resource {} version {} to {}",
                    relativeResourceDir, version, archiveDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot archive EventCatalog resource " + relativeResourceDir
                    + " version " + version, e);
        }
    }

    private boolean isNestedResourceCollection(Path entry) {
        return Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                && VERSIONED_COLLECTIONS.contains(fileName(entry));
    }

    private boolean isSafeVersionDirectory(String version) {
        return version != null
                && !version.isBlank()
                && !".".equals(version)
                && !"..".equals(version)
                && version.indexOf('/') < 0
                && version.indexOf('\\') < 0;
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            Files.copy(source, target, LinkOption.NOFOLLOW_LINKS,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path destination = target.resolve(source.relativize(dir));
                Files.createDirectories(destination);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path destination = target.resolve(source.relativize(file));
                Files.copy(file, destination, LinkOption.NOFOLLOW_LINKS,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private String readFrontmatterVersion(File file) {
        try {
            String content = Files.readString(file.toPath());
            String yaml = extractFrontmatterYaml(content);
            if (yaml == null) return null;
            Map<String, Object> frontmatter = yamlMapper.readValue(yaml, Map.class);
            Object version = frontmatter.get("version");
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
            Map<String, Object> frontmatter = yamlMapper.readValue(yaml, Map.class);
            Object version = frontmatter.get("version");
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

    private String fileName(Path path) {
        Path name = path.getFileName();
        return name != null ? name.toString() : "";
    }

    // -------------------------------------------------------------------------
    // Output cleanup
    // -------------------------------------------------------------------------

    /**
     * Deletes all files and directories under {@code targetFolder} except those
     * that are inside a {@code versioned/} sub-tree.
     */
    private void cleanOutputFolder() {
        if (!targetFolder.exists()) return;

        Path root = targetFolder.toPath().toAbsolutePath().normalize();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) return FileVisitResult.CONTINUE;
                    if ("versioned".equals(fileName(dir))) {
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
                    if (dir.equals(root)) return FileVisitResult.CONTINUE;
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException ignored) {
                        // The directory still contains historical versioned content.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Cannot clean EventCatalog output folder " + root, e);
        }
    }

    // -------------------------------------------------------------------------
    // File writing
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
