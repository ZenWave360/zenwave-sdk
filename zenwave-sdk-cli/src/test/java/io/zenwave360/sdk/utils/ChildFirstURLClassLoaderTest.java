package io.zenwave360.sdk.utils;

import java.net.URL;

import io.zenwave360.sdk.zdl.ProjectTemplates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChildFirstURLClassLoaderTest {

    @Test
    public void sdkTypesFromParentEvenWhenDepsJarHasACopy() throws Exception {
        // a --deps jar that brings the SDK again as a transitive: here, the SDK classes folder itself
        URL sdkCopy = ProjectTemplates.class.getProtectionDomain().getCodeSource().getLocation();
        try (var classLoader = new MavenLoader.ChildFirstURLClassLoader(new URL[] {sdkCopy}, getClass().getClassLoader())) {
            Assertions.assertSame(ProjectTemplates.class, classLoader.loadClass(ProjectTemplates.class.getName()));
            Assertions.assertSame(org.slf4j.Logger.class, classLoader.loadClass(org.slf4j.Logger.class.getName()));
        }
    }

    @Test
    public void otherClassesChildFirst() throws Exception {
        // picocli is on the parent too, but outside the parent-first packages: the --deps copy wins
        URL picocliJar = picocli.CommandLine.class.getProtectionDomain().getCodeSource().getLocation();
        try (var classLoader = new MavenLoader.ChildFirstURLClassLoader(new URL[] {picocliJar}, getClass().getClassLoader())) {
            Assertions.assertSame(classLoader, classLoader.loadClass(picocli.CommandLine.class.getName()).getClassLoader());
        }
    }
}
