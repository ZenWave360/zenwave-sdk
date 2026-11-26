package io.zenwave360.sdk;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Templates classes and .hbs files from jars that are only on the project class loader (CLI --deps, Maven
 * includeProjectClasspath), not on the SDK class path.
 */
public class ProjectClassLoaderTest {

    public static class TemplatesProbe extends Generator {
        static Object templatesSeen;
        static String renderedSeen;

        public ProjectTemplates templates;

        @Override
        public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
            templatesSeen = templates;
            renderedSeen = getTemplateEngine()
                    .processTemplate(Map.of("name", "deps"), new TemplateInput("probe/Probe.txt", "Probe.txt"))
                    .getContent();
            return new GeneratedProjectFiles();
        }
    }

    @Test
    public void templatesClassAndHbsFromProjectClassLoader(@TempDir Path tempDir) throws Exception {
        Path classes = compileDepsTemplates(tempDir);
        // the deps template includes a partial that lives in the SDK class path
        Files.writeString(classes.resolve("probe/Probe.txt.hbs"), "hello {{name}}, {{> io/zenwave360/sdk/probe-sdk-partial}}");

        try (URLClassLoader depsClassLoader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader())) {
            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("probe.DepsTemplates"));
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

            Plugin plugin = new Plugin()
                    .withChain(TemplatesProbe.class)
                    .withProjectClassLoader(depsClassLoader)
                    .withTargetFolder("target" + File.separator + "project-class-loader-test")
                    .withOption("templates", "new probe.DepsTemplates");
            new MainGenerator().generate(plugin);

            Assertions.assertEquals("probe.DepsTemplates", TemplatesProbe.templatesSeen.getClass().getName());
            Assertions.assertSame(depsClassLoader, TemplatesProbe.templatesSeen.getClass().getClassLoader());
            Assertions.assertEquals("hello deps, sdk deps", TemplatesProbe.renderedSeen);
            Assertions.assertSame(contextClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    public void templatesFromSdkClassPathWithoutProjectClassLoader() throws Exception {
        Plugin plugin = new Plugin()
                .withChain(TemplatesProbe.class)
                .withTargetFolder("target" + File.separator + "project-class-loader-test")
                .withOption("templates", "new " + ProjectTemplates.class.getName());
        Assertions.assertThrows(RuntimeException.class, () -> new MainGenerator().generate(plugin),
                "probe/Probe.txt.hbs is not on the SDK class path");
        Assertions.assertSame(ProjectTemplates.class, TemplatesProbe.templatesSeen.getClass());
    }

    static Path compileDepsTemplates(Path tempDir) throws Exception {
        Path src = tempDir.resolve("src/probe/DepsTemplates.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "package probe; public class DepsTemplates extends " + ProjectTemplates.class.getName() + " {}");
        Path classes = tempDir.resolve("classes");
        Files.createDirectories(classes.resolve("probe"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null,
                "-cp", System.getProperty("java.class.path"), "-d", classes.toString(), src.toString());
        Assertions.assertEquals(0, rc);
        return classes;
    }
}
