package io.zenwave360.sdk.utils;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassLoaderUtilsTest {

    @Test
    public void defaultsToSdkClassLoader() {
        Assertions.assertNull(ClassLoaderUtils.getProjectClassLoader());
        Assertions.assertSame(ClassLoaderUtils.class.getClassLoader(), ClassLoaderUtils.getClassLoader());
    }

    @Test
    public void scopedNestedAndRestored() throws Exception {
        try (var outer = new URLClassLoader(new URL[0], getClass().getClassLoader());
                var inner = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            ClassLoaderUtils.withProjectClassLoader(outer, () -> {
                Assertions.assertSame(outer, ClassLoaderUtils.getClassLoader());
                // null keeps the current one: nested generations without their own loader inherit it
                Assertions.assertSame(outer, ClassLoaderUtils.withProjectClassLoader(null, ClassLoaderUtils::getProjectClassLoader));
                Assertions.assertSame(inner, ClassLoaderUtils.withProjectClassLoader(inner, ClassLoaderUtils::getProjectClassLoader));
                Assertions.assertSame(outer, ClassLoaderUtils.getProjectClassLoader());
                return null;
            });
            Assertions.assertNull(ClassLoaderUtils.getProjectClassLoader());
        }
    }

    @Test
    public void restoredOnFailure() {
        try (var classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            Assertions.assertThrows(IllegalStateException.class, () -> ClassLoaderUtils.withProjectClassLoader(classLoader, () -> {
                throw new IllegalStateException("boom");
            }));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNull(ClassLoaderUtils.getProjectClassLoader());
    }

    @Test
    public void perThread() throws Exception {
        try (var classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            ClassLoaderUtils.withProjectClassLoader(classLoader, () -> {
                var seen = new ClassLoader[1];
                var thread = new Thread(() -> seen[0] = ClassLoaderUtils.getProjectClassLoader());
                thread.start();
                thread.join();
                Assertions.assertNull(seen[0]);
                return null;
            });
        }
    }
}
