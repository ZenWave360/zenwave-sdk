package io.zenwave360.sdk.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class MavenLoaderTest {

    @Test
    void testFindJBangDependencies() {
        // Given
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");
        List<String> repos = List.of("https://repo1.maven.org/maven2/");

        // When
        List<URL> urls = MavenLoader.findJBangDependencies(dependencies, repos);

        // Then
        Assertions.assertNotNull(urls);
        Assertions.assertFalse(urls.isEmpty());
        Assertions.assertTrue(urls.stream().anyMatch(url -> url.toString().contains("commons-lang3")));
    }

    @Test
    void testFindJBangLocalDependencies() {
        // Given
        List<String> dependencies = List.of("io.example.asyncapi.shoppingcart:shopping-cart-apis:1.0.0");

        // When
        List<URL> urls = MavenLoader.findJBangDependencies(dependencies, null);

        // Then
        Assertions.assertNotNull(urls);
        Assertions.assertFalse(urls.isEmpty());
        Assertions.assertTrue(urls.stream().anyMatch(url -> url.toString().contains("shopping-cart-apis")));
    }


    @Test
    void testFindJBangDependenciesWithNullRepos() {
        // Given
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");

        // When
        List<URL> urls = MavenLoader.findJBangDependencies(dependencies, null);

        // Then
        Assertions.assertNotNull(urls);
        Assertions.assertFalse(urls.isEmpty());
    }

    @Test
    void testLoadJBangDependencies() {
        // Given
        List<String> dependencies = List.of("org.apache.commons:commons-lang3:3.12.0");
        List<String> repos = List.of("https://repo1.maven.org/maven2/");

        // When
        ClassLoader projectClassLoader = MavenLoader.loadJBangDependencies(dependencies, repos);

        // Then
        Assertions.assertInstanceOf(URLClassLoader.class, projectClassLoader);
    }

    @Test
    void testLoadJBangDependenciesWithEmptyLists() {
        // Given
        List<String> dependencies = new ArrayList<>();
        List<String> repos = new ArrayList<>();

        // When & Then - should not throw exception
        Assertions.assertDoesNotThrow(() ->
            MavenLoader.loadJBangDependencies(dependencies, repos)
        );
    }
}
