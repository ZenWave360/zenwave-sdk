package io.zenwave360.sdk.utils;

import dev.jbang.dependencies.MavenRepo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MavenLoaderTest {

    @Test
    void testFindJBangDependencies() {
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");
        List<String> repos = List.of("https://repo1.maven.org/maven2/");

        List<URL> urls = MavenLoader.findJBangDependencies(dependencies, repos);

        Assertions.assertNotNull(urls);
        Assertions.assertFalse(urls.isEmpty());
        Assertions.assertTrue(urls.stream().anyMatch(url -> url.toString().contains("commons-lang3")));
    }

    @Test
    void testFindJBangDependenciesWithNullRepos() {
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");

        List<URL> urls = MavenLoader.findJBangDependencies(dependencies, null);

        Assertions.assertNotNull(urls);
        Assertions.assertFalse(urls.isEmpty());
    }

    @Test
    void testLoadJBangDependencies() {
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");
        List<String> repos = List.of("https://repo1.maven.org/maven2/");

        ClassLoader projectClassLoader = MavenLoader.loadJBangDependencies(dependencies, repos);

        Assertions.assertInstanceOf(URLClassLoader.class, projectClassLoader);
    }

    @Test
    void testLoadJBangDependenciesWithEmptyLists() {
        List<String> dependencies = new ArrayList<>();
        List<String> repos = new ArrayList<>();

        Assertions.assertDoesNotThrow(() ->
            MavenLoader.loadJBangDependencies(dependencies, repos)
        );
    }

    @Test
    void testResolveMavenReposParsesIdUrlAndKeepsFallbacks() {
        List<MavenRepo> repos = MavenLoader.resolveMavenRepos(
                List.of("github=https://maven.pkg.github.com/org/repo"),
                null);

        Assertions.assertEquals("github", repos.get(0).getId());
        Assertions.assertEquals("https://maven.pkg.github.com/org/repo", repos.get(0).getUrl());
        Assertions.assertTrue(repos.stream().anyMatch(repo -> "central".equals(repo.getId())));
        Assertions.assertTrue(repos.stream().anyMatch(repo ->
                MavenLoader.SNAPSHOTS_URL.equals(normalize(repo.getUrl()))));
    }

    @Test
    void testResolveMavenReposUsesJBangConfig() {
        List<MavenRepo> repos = MavenLoader.resolveMavenRepos(
                null,
                "central,github=https://maven.pkg.github.com/arcadia-editions/arcadia-editions-modulith-api");

        Assertions.assertEquals("central", repos.get(0).getId());
        Assertions.assertEquals(MavenLoader.CENTRAL_URL, normalize(repos.get(0).getUrl()));
        Assertions.assertEquals("github", repos.get(1).getId());
        Assertions.assertEquals(
                "https://maven.pkg.github.com/arcadia-editions/arcadia-editions-modulith-api",
                repos.get(1).getUrl());
    }

    @Test
    void testResolveMavenReposDoesNotDuplicateCentral() {
        List<MavenRepo> repos = MavenLoader.resolveMavenRepos(
                List.of("central"),
                "central,github=https://maven.pkg.github.com/org/repo");

        long centrals = repos.stream()
                .filter(repo -> MavenLoader.CENTRAL_URL.equals(normalize(repo.getUrl())))
                .count();
        Assertions.assertEquals(1, centrals);
    }

    @Test
    void testJBangPropertiesNearestWinsOverUserConfig(@TempDir Path tempDir) throws Exception {
        Path localRoot = tempDir.resolve("home");
        Path userConfig = localRoot.resolve(".jbang");
        Path project = localRoot.resolve("workspace").resolve("service");
        Files.createDirectories(userConfig);
        Files.createDirectories(project);

        Files.writeString(userConfig.resolve("jbang.properties"),
                "run.repos=github=https://maven.pkg.github.com/org/from-user\n");
        Files.writeString(project.resolve("jbang.properties"),
                "run.repos=github=https://maven.pkg.github.com/org/from-project\n");

        String value = MavenLoader.jbangConfiguredReposValue(project, localRoot, userConfig);

        Assertions.assertEquals("github=https://maven.pkg.github.com/org/from-project", value.trim());
    }

    @Test
    void testJBangPropertiesFallsBackToReposKey(@TempDir Path tempDir) throws Exception {
        Path localRoot = tempDir.resolve("home");
        Path userConfig = localRoot.resolve(".jbang");
        Path project = localRoot.resolve("workspace").resolve("service");
        Files.createDirectories(userConfig);
        Files.createDirectories(project);

        Files.writeString(userConfig.resolve("jbang.properties"),
                "repos=github=https://maven.pkg.github.com/org/from-user\n");

        String value = MavenLoader.jbangConfiguredReposValue(project, localRoot, userConfig);

        Assertions.assertEquals("github=https://maven.pkg.github.com/org/from-user", value.trim());
    }

    @Test
    void testJBangDotDirPropertiesAreDiscovered(@TempDir Path tempDir) throws Exception {
        Path localRoot = tempDir.resolve("home");
        Path userConfig = localRoot.resolve(".jbang");
        Path project = localRoot.resolve("workspace").resolve("service");
        Files.createDirectories(userConfig);
        Files.createDirectories(project.resolve(".jbang"));

        Files.writeString(project.resolve(".jbang").resolve("jbang.properties"),
                "run.repos=acme=https://maven.acme.local/maven\n");

        String value = MavenLoader.jbangConfiguredReposValue(project, localRoot, userConfig);

        Assertions.assertEquals("acme=https://maven.acme.local/maven", value.trim());
    }

    private static String normalize(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
