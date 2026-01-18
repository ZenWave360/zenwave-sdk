package io.zenwave360.sdk.utils;

import dev.jbang.dependencies.DependencyUtil;
import dev.jbang.dependencies.MavenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class MavenLoader {

    private static Logger log = LoggerFactory.getLogger(MavenLoader.class);

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
        List<MavenRepo> mavenRepos = new ArrayList<>();
        if (repos != null) {
            mavenRepos.addAll(repos.stream().map(repo -> new MavenRepo(repo, repo)).toList());
        }
        mavenRepos.add(new MavenRepo("central", "https://repo1.maven.org/maven2/"));
        mavenRepos.add(new MavenRepo("snapshots", "https://s01.oss.sonatype.org/content/repositories/snapshots/"));

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
