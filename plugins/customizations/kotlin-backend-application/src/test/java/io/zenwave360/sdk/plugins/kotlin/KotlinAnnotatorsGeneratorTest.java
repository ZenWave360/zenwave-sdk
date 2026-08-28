package io.zenwave360.sdk.plugins.kotlin;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;

/**
 * The annotator framework is generator agnostic: annotations are computed once in ZDLProcessor and
 * {@code {{annotate}}} is registered globally on HandlebarsEngine, so the Kotlin templates get the
 * same output as the Java ones.
 */
public class KotlinAnnotatorsGeneratorTest {

    @Test
    public void kotlin_templates_render_jmolecules_and_jspecify_annotations() throws Exception {
        String targetFolder = "target/projects/kotlin-annotators";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false)
                .withOption("useJSpecify", true)
                .withOption("useJMolecules", true);

        new MainGenerator().generate(plugin);

        var base = "src/main/kotlin/io/zenwave360/examples/kotlin/";
        var customer = read(targetFolder, base + "core/domain/Customer.kt");
        var address = read(targetFolder, base + "core/domain/Address.kt");
        var repository = read(targetFolder, base + "core/outbound/jpa/CustomerRepository.kt");
        var servicePort = read(targetFolder, base + "core/inbound/CustomerService.kt");
        var serviceImpl = read(targetFolder, base + "core/application/CustomerServiceImpl.kt");

        Assertions.assertTrue(customer.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));
        Assertions.assertTrue(address.contains("@org.jmolecules.ddd.annotation.Entity"));
        Assertions.assertTrue(repository.contains("@org.jmolecules.ddd.annotation.Repository"));

        // same javaService, split across two artifacts, exactly as in the Java templates
        Assertions.assertTrue(servicePort.contains("@org.jmolecules.architecture.hexagonal.PrimaryPort"));
        Assertions.assertTrue(serviceImpl.contains("@org.jmolecules.architecture.hexagonal.Application"));
        Assertions.assertFalse(serviceImpl.contains("PrimaryPort"));

        // unscoped annotations reach both
        Assertions.assertTrue(servicePort.contains("@org.jspecify.annotations.NullMarked"));
        Assertions.assertTrue(serviceImpl.contains("@org.jspecify.annotations.NullMarked"));
    }

    @Test
    public void kotlin_generates_the_jmolecules_architecture_test() throws Exception {
        String targetFolder = "target/projects/kotlin-annotators-archunit";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false)
                .withOption("useJMolecules", true);

        new MainGenerator().generate(plugin);

        var test = read(targetFolder, "src/test/kotlin/io/zenwave360/examples/kotlin/JMoleculesArchitectureTest.kt");
        Assertions.assertTrue(test.contains("JMoleculesDddRules.annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier"));
        // default layout derives HEXAGONAL
        Assertions.assertTrue(test.contains("ensureHexagonal(VerificationDepth.STRICT)"));
        // shipped enabled; projects opt out by editing the generated file, which is never overwritten
        Assertions.assertFalse(test.contains("// @ArchTest"));
        // the analysed package must be resolved, not the empty string
        Assertions.assertTrue(test.contains("packages = [\"io.zenwave360.examples.kotlin\"]"));

        var architectureTest = read(targetFolder, "src/test/kotlin/io/zenwave360/examples/kotlin/ArchitectureTest.kt");
        Assertions.assertTrue(architectureTest.contains("packages = [\"io.zenwave360.examples.kotlin\"]"));
        Assertions.assertTrue(architectureTest.contains("\"io.zenwave360.examples.kotlin..\""));
    }

    private String read(String targetFolder, String path) throws Exception {
        var file = new File(targetFolder, path);
        Assertions.assertTrue(file.exists(), "expected generated file " + path);
        return new String(Files.readAllBytes(file.toPath()));
    }
}
