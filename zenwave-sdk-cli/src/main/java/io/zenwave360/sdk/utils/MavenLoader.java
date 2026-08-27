package io.zenwave360.sdk.utils;

import dev.jbang.dependencies.DependencyUtil;
import dev.jbang.dependencies.MavenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

public class MavenLoader {

    private static Logger log = LoggerFactory.getLogger(MavenLoader.class);

    static final String CENTRAL_URL = "https://repo1.maven.org/maven2/";
    static final String SNAPSHOTS_URL = "https://s01.oss.sonatype.org/content/repositories/snapshots/";

    public static URLClassLoader loadJBangDependencies(List<String> dependencies, List<String> repos) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        log.info("Loading {} dependencies: {}", dependencies.size(), dependencies);
        var urls = findJBangDependencies(dependencies, repos);
        log.info("Found {} urls: {}", urls.size(), urls);
        return new ChildFirstURLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );
    }

    public static List<URL> findJBangDependencies(List<String> dependencies, List<String> repos) {
        List<MavenRepo> mavenRepos = resolveMavenRepos(repos);
        log.info("Resolving dependencies from repositories: {}", mavenRepos);

        var modularClassPath = DependencyUtil.resolveDependencies(dependencies, mavenRepos, false, false, false, false, false);
        List<String> files = modularClassPath.getClassPaths();
        List<URL> urls = files.stream().map(f -> {
            try {
                return new File(f).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList();
        return urls;
    }

    /**
     * Extra {@code --repos} first, then JBang {@code run.repos}/{@code repos}, then central and snapshots.
     */
    static List<MavenRepo> resolveMavenRepos(List<String> extraRepos) {
        return resolveMavenRepos(extraRepos, jbangConfiguredReposValue());
    }

    static List<MavenRepo> resolveMavenRepos(List<String> extraRepos, String jbangReposConfig) {
        LinkedHashMap<String, MavenRepo> byUrl = new LinkedHashMap<>();
        addRepoRefs(byUrl, extraRepos);
        addRepoRefs(byUrl, splitRepoRefs(jbangReposConfig));
        addIfAbsent(byUrl, DependencyUtil.toMavenRepo("central"));
        addIfAbsent(byUrl, new MavenRepo("snapshots", SNAPSHOTS_URL));
        return new ArrayList<>(byUrl.values());
    }

    static String jbangConfiguredReposValue() {
        String override = System.getenv("JBANG_CONFIG");
        Path startDir = Paths.get("").toAbsolutePath().normalize();
        if (override != null && !override.isBlank()) {
            Path cfgFile = startDir.resolve(override).normalize();
            if (Files.isRegularFile(cfgFile) && Files.isReadable(cfgFile)) {
                return reposValueFromProperties(loadProperties(cfgFile));
            }
            return null;
        }
        return jbangConfiguredReposValue(startDir, localRootDir(), userConfigDir());
    }

    static String jbangConfiguredReposValue(Path startDir, Path localRoot, Path userConfigDir) {
        return reposValueFromProperties(loadMergedJBangProperties(startDir, localRoot, userConfigDir));
    }

    static Properties loadMergedJBangProperties(Path startDir, Path localRoot, Path userConfigDir) {
        Properties merged = new Properties();
        for (Path file : discoverJBangPropertiesFiles(startDir, localRoot, userConfigDir)) {
            merged.putAll(loadProperties(file));
        }
        return merged;
    }

    static List<Path> discoverJBangPropertiesFiles(Path startDir, Path localRoot, Path userConfigDir) {
        List<Path> nearestFirst = new ArrayList<>();
        Path dir = startDir == null ? null : startDir.toAbsolutePath().normalize();
        Path root = localRoot == null ? null : localRoot.toAbsolutePath().normalize();
        while (dir != null && (root == null || !dir.equals(root))) {
            addIfReadable(nearestFirst, dir.resolve("jbang.properties"));
            addIfReadable(nearestFirst, dir.resolve(".jbang").resolve("jbang.properties"));
            dir = dir.getParent();
        }
        if (userConfigDir != null) {
            addIfReadable(nearestFirst, userConfigDir.resolve("jbang.properties"));
        }
        Collections.reverse(nearestFirst);
        return nearestFirst;
    }

    private static String reposValueFromProperties(Properties props) {
        String value = props.getProperty("run.repos");
        if (value == null || value.isBlank()) {
            value = props.getProperty("repos");
        }
        return value;
    }

    private static Properties loadProperties(Path file) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            log.debug("Could not read JBang config {}", file, e);
        }
        return props;
    }

    private static Path localRootDir() {
        String localRoot = System.getenv("JBANG_LOCAL_ROOT");
        if (localRoot != null && !localRoot.isBlank()) {
            return Paths.get(localRoot);
        }
        return Paths.get(System.getProperty("user.home"));
    }

    private static Path userConfigDir() {
        String jbangDir = System.getenv("JBANG_DIR");
        if (jbangDir != null && !jbangDir.isBlank()) {
            return Paths.get(jbangDir);
        }
        return Paths.get(System.getProperty("user.home")).resolve(".jbang");
    }

    private static void addRepoRefs(LinkedHashMap<String, MavenRepo> byUrl, List<String> repoRefs) {
        if (repoRefs == null) {
            return;
        }
        for (String repoRef : repoRefs) {
            if (repoRef == null || repoRef.isBlank()) {
                continue;
            }
            addIfAbsent(byUrl, DependencyUtil.toMavenRepo(repoRef.trim()));
        }
    }

    private static List<String> splitRepoRefs(String repos) {
        if (repos == null || repos.isBlank()) {
            return List.of();
        }
        List<String> refs = new ArrayList<>();
        for (String part : repos.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                refs.add(trimmed);
            }
        }
        return refs;
    }

    private static void addIfAbsent(LinkedHashMap<String, MavenRepo> byUrl, MavenRepo repo) {
        String url = normalizeUrl(repo.getUrl());
        byUrl.putIfAbsent(url, repo);
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private static void addIfReadable(List<Path> files, Path file) {
        if (Files.isRegularFile(file) && Files.isReadable(file)) {
            files.add(file);
        }
    }

    private static class ChildFirstURLClassLoader extends URLClassLoader {
        public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    c = super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
