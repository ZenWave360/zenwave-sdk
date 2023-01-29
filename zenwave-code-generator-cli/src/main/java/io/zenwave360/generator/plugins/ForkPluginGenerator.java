package io.zenwave360.generator.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.Generator;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.NamingUtils;

public class ForkPluginGenerator implements Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Plugin Configuration class to fork", required = true)
    public String sourcePluginClassName;

    @DocumentedOption(description = "New Plugin Configuration class. It will be used for class name, package and maven groupId.", required = true)
    public String targetPluginClassName;

    @DocumentedOption(description = "Download URL for the source code of original plugin in zip format", required = false)
    public URL downloadURL = new URL("https://github.com/ZenWave360/zenwave-code-generator/archive/refs/tags/v0.9.12.zip");

    @DocumentedOption
    public String targetFolder;

    public ForkPluginGenerator() throws MalformedURLException {}

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        try {

            File zipFile = download(downloadURL);
            File repoDirectory = uncompress(zipFile);
            File javaFile = findJavaFileInFolder(repoDirectory, sourcePluginClassName);
            File mavenModuleFile = findMavenModuleRootFolder(javaFile);
            File targetFolderFile = new File(targetFolder);
            copyFolders(mavenModuleFile, targetFolderFile);
            renameArtifactAndGroupId(new File(targetFolderFile, "pom.xml"), sourcePluginClassName, targetPluginClassName);
            movePackageFolders(targetFolderFile, sourcePluginClassName, targetPluginClassName);
            deleteEmptyFolders(targetFolderFile);
            searchAndReplace(targetFolderFile, sourcePluginClassName, targetPluginClassName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    final Pattern packageClassSplitPattern = Pattern.compile("(.*)\\.(\\w+)", Pattern.MULTILINE);
    final Pattern groupIdPattern = Pattern.compile("^(\\s*)<groupId>.*<\\/groupId>", Pattern.MULTILINE);
    final Pattern artifactIdPattern = Pattern.compile("^(\\s*)<artifactId>.*<\\/artifactId>", Pattern.MULTILINE);

    protected void copyFolders(File source, File target) throws IOException {
        log.debug("Copying {} to {}", source, target);
        target.mkdirs();
        FileUtils.copyDirectory(source, target);
    }

    protected void movePackageFolders(File folder, String sourcePluginClassName, String targetPluginClassName) throws IOException {
        log.debug("Renaming packages from {} to {}", sourcePluginClassName, targetPluginClassName);
        String[] sourcePackageAndClass = splitPackageAndClass(sourcePluginClassName);
        String[] targetPackageAndClass = splitPackageAndClass(targetPluginClassName);

        File srcFolder = new File(folder, "src/main/java");
        File testsFolder = new File(folder, "src/test/java");
        File resourcesFolder = new File(folder, "src/main/resources");
        File testsResourcesFolder = new File(folder, "src/main/resources");

        String sourcePackageDir = sourcePackageAndClass[0].replaceAll("\\.", "/");
        String targetPackageDir = targetPackageAndClass[0].replaceAll("\\.", "/");
        File sourcePackageFolder = new File(srcFolder, sourcePackageDir);
        File targetPackageFolder = new File(srcFolder, targetPackageDir);
        File sourcePackageTestsFolder = new File(testsFolder, sourcePackageDir);
        File targetPackageTestsFolder = new File(testsFolder, targetPackageDir);
        File sourceResourcesPackageFolder = new File(resourcesFolder, sourcePackageDir);
        File targetResourcesPackageFolder = new File(resourcesFolder, targetPackageDir);
        File sourceTestsResourcesPackageFolder = new File(testsResourcesFolder, sourcePackageDir);
        File targetTestsResourcesPackageFolder = new File(testsResourcesFolder, targetPackageDir);

        targetPackageFolder.mkdirs();
        Collection<File> javaFiles = FileUtils.listFiles(sourcePackageFolder, new String[] {"java"}, true);
        for (File javaFile : javaFiles) {
            log.debug("Moving {} to folder {}", javaFile, targetPackageFolder);
            String relativeFileName = asRelative(sourcePackageFolder.getPath(), javaFile.getPath());
            relativeFileName = renameTargetClass(relativeFileName, sourcePackageAndClass[1], targetPackageAndClass[1]);
            moveFile(javaFile, new File(targetPackageFolder, relativeFileName));
        }

        targetPackageTestsFolder.mkdirs();
        Collection<File> testFiles = FileUtils.listFiles(sourcePackageTestsFolder, null, true);
        for (File testFile : testFiles) {
            log.debug("Moving {} to folder {}", testFile, targetPackageTestsFolder);
            String relativeFileName = asRelative(sourcePackageTestsFolder.getPath(), testFile.getPath());
            relativeFileName = renameTargetClass(relativeFileName, sourcePackageAndClass[1], targetPackageAndClass[1]);
            moveFile(testFile, new File(targetPackageTestsFolder, relativeFileName));
        }

        targetResourcesPackageFolder.mkdirs();
        Collection<File> resourceFiles = FileUtils.listFiles(sourceResourcesPackageFolder, null, true);
        for (File resourceFile : resourceFiles) {
            log.debug("Moving {} to folder {}", resourceFile, targetResourcesPackageFolder);
            String relativeFileName = asRelative(sourceResourcesPackageFolder.getPath(), resourceFile.getPath());
            relativeFileName = renameTargetClass(relativeFileName, sourcePackageAndClass[1], targetPackageAndClass[1]);
            moveFile(resourceFile, new File(targetResourcesPackageFolder, relativeFileName));
        }

        targetTestsResourcesPackageFolder.mkdirs();
        Collection<File> testsResourceFiles = FileUtils.listFiles(sourceTestsResourcesPackageFolder, null, true);
        for (File testsResourceFile : testsResourceFiles) {
            log.debug("Moving {} to folder {}", testsResourceFile, targetTestsResourcesPackageFolder);
            String relativeFileName = asRelative(sourceTestsResourcesPackageFolder.getPath(), testsResourceFile.getPath());
            relativeFileName = renameTargetClass(relativeFileName, sourcePackageAndClass[1], targetPackageAndClass[1]);
            moveFile(testsResourceFile, new File(targetTestsResourcesPackageFolder, relativeFileName));
        }
    }

    private void moveFile(File src, File target) throws IOException {
        if (src.equals(target)) {
            log.debug("Skip moving file to itself {}", src);
            return;
        }
        FileUtils.moveFile(src, target);
    }

    private String asRelative(String root, String nested) {
        return nested.replace(root + File.separator, "");
    }

    private String renameTargetClass(String relativeFileName, String sourceClass, String targetClass) {
        String prefix = relativeFileName.contains(File.separator) ? File.separator : "";
        return relativeFileName.replace(prefix + sourceClass + ".java", prefix + targetClass + ".java");
    }

    private void searchAndReplace(File targetFolderFile, String sourcePluginClassName, String targetPluginClassName) throws IOException {
        String[] sourcePackageAndClass = splitPackageAndClass(sourcePluginClassName);
        String[] targetPackageAndClass = splitPackageAndClass(targetPluginClassName);
        List replacements = List.of(
                // Pair.of(sourcePluginClassName, targetPluginClassName),
                Pair.of(sourcePackageAndClass[0], targetPackageAndClass[0]),
                Pair.of(sourcePackageAndClass[1], targetPackageAndClass[1]),
                Pair.of(sourcePluginClassName.replaceAll("\\.", "/"), targetPluginClassName.replaceAll("\\.", "/")),
                Pair.of(sourcePackageAndClass[0].replaceAll("\\.", "/"), targetPackageAndClass[0].replaceAll("\\.", "/")));
        log.debug("Replacing packages and class names in files {}", replacements);
        searchAndReplaceInFolder(targetFolderFile, replacements);
    }

    protected void searchAndReplaceInFolder(File root, List<Pair<String, String>> replacements) throws IOException {
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                searchAndReplaceInFolder(file, replacements);
            } else if (file.isFile() && !file.getName().contentEquals("pom.xml")) {
                searchAndReplaceInFile(file, replacements);
            }
        }
    }

    protected void searchAndReplaceInFile(File file, List<Pair<String, String>> replacements) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        for (Pair<String, String> replacement : replacements) {
            content = StringUtils.replace(content, replacement.getKey(), replacement.getValue());
        }
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

    protected void renameArtifactAndGroupId(File pomFile, String sourcePluginClassName, String targetPluginClassName) throws IOException {
        log.debug("Renaming artifactId and groupId in pom {}", pomFile);
        String[] sourcePackageAndClass = splitPackageAndClass(sourcePluginClassName);
        String[] targetPackageAndClass = splitPackageAndClass(targetPluginClassName);

        String targetArtifactId = NamingUtils.kebabCase(targetPackageAndClass[1]).replace("-configuration", "");

        String pom = Files.readString(Path.of(pomFile.toURI()));
        String artifactIdMatch = findMatchWithShorterIndent(pom, artifactIdPattern);
        String groupIdMatch = findMatchWithShorterIndent(pom, groupIdPattern);
        pom = replaceLine(pom, artifactIdMatch, "    <artifactId>" + targetArtifactId + "</artifactId>");
        pom = replaceLine(pom, groupIdMatch, "    <groupId>" + targetPackageAndClass[0] + "</groupId>");
        Files.writeString(Path.of(pomFile.toURI()), pom);
    }

    public String replaceLine(String value, String regex, String replacement) {
        return Pattern.compile("^" + regex.replace("/", "\\/") + "$", Pattern.MULTILINE).matcher(value).replaceAll(replacement);
    }

    private String[] splitPackageAndClass(String fullClassName) {
        Matcher matcher = packageClassSplitPattern.matcher(fullClassName);
        if (matcher.find()) {
            return new String[] {matcher.group(1), matcher.group(2)};
        }
        return new String[2];
    }

    private String findMatchWithShorterIndent(String pom, Pattern pattern) {
        String shorterMatch = null;
        int shorterIndent = Integer.MAX_VALUE;
        Matcher matcher = pattern.matcher(pom);
        while (matcher.find()) {
            int indent = matcher.group(1).replaceAll("\t", "  ").length();
            if (indent < shorterIndent) {
                shorterIndent = indent;
                shorterMatch = matcher.group();
            }
        }
        return shorterMatch;
    }

    protected File findMavenModuleRootFolder(File file) {
        File pomFile = new File(file, "pom.xml");
        if (pomFile.exists()) {
            log.debug("Found maven module root at {}", pomFile.getParentFile());
            return pomFile.getParentFile();
        } else if (file.getParentFile().exists()) {
            return findMavenModuleRootFolder(file.getParentFile());
        }
        return null;
    }

    protected File findJavaFileInFolder(File repoDirectory, String javaClassName) {
        File javaFile = new File(repoDirectory, "src/main/java/" + javaClassName.replace('.', '/') + ".java");
        if (javaFile.exists()) {
            return javaFile;
        }
        for (File file : repoDirectory.listFiles()) {
            if (file.isDirectory()) {
                javaFile = findJavaFileInFolder(file, javaClassName);
                if (javaFile != null) {
                    return javaFile;
                }
            }
        }
        return null;
    }

    protected void deleteEmptyFolders(File root) throws IOException {
        List<Path> files = Files.walk(root.toPath()).sorted().collect(Collectors.toList());
        Collections.reverse(files);
        for (Path path : files) {
            if (Files.isDirectory(path) && Files.list(path).count() == 0) {
                Files.delete(path);
            }
        }
    }

    protected File download(URL downloadURL) throws IOException {
        File zipFile = File.createTempFile("zenwave-code-generator-repo-", ".zip");
        log.debug("Downloading {} to {}", downloadURL, zipFile.getAbsoluteFile());
        FileUtils.copyURLToFile(downloadURL, zipFile);
        return zipFile;
    }

    protected File uncompress(File zipFile) throws IOException {
        File outputDir = File.createTempFile("zenwave-code-generator-repo-", "");
        outputDir.delete();
        outputDir.mkdir();
        log.debug("Uncompressing {} to {}", zipFile, outputDir);

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(outputDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        return outputDir;
    }

    /**
     * This method guards against writing files to the file system outside of the target folder. This vulnerability is called Zip Slip, and we can read more about it here https://snyk.io/research/zip-slip-vulnerability.
     * 
     * @param destinationDir
     * @param zipEntry
     * @return
     * @throws IOException
     */
    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
