package io.zenwave360.sdk.plugins;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;

/**
 * End to end checks for the ZDL annotator framework: the same model element must be annotated
 * differently in each of the artifacts it generates, and generated tests must stay clean.
 */
public class AnnotatorsGeneratorTest {

    /** Every event in this model is {@code @asyncapi}, so no EventPublisher is generated. */
    private Plugin plugin(String targetFolder) {
        return plugin(targetFolder, "customer-address-relational.zdl");
    }

    /** This model has plain (non {@code @asyncapi}) events, so domain events and an EventPublisher exist. */
    private Plugin domainEventsPlugin(String targetFolder) {
        return plugin(targetFolder, "customer-address-aggregate-and-entity-lifecycle.zdl");
    }

    private Plugin plugin(String targetFolder, String zdl) {
        return new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/" + zdl)
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);
    }

    private String read(String targetFolder, String path) throws Exception {
        var file = new File(targetFolder, path);
        Assertions.assertTrue(file.exists(), "expected generated file " + path);
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void jmolecules_annotates_each_artifact_differently() throws Exception {
        String targetFolder = "target/zdl/test_annotators_jmolecules";
        new MainGenerator().generate(plugin(targetFolder)
                .withOption("useJMolecules", true));

        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Customer.java");
        var address = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Address.java");
        var repository = read(targetFolder, "src/main/java/io/zenwave360/example/core/outbound/jpa/CustomerRepository.java");
        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/core/inbound/CustomerService.java");
        var serviceImpl = read(targetFolder, "src/main/java/io/zenwave360/example/core/application/CustomerServiceImpl.java");

        // Customer is an @aggregate, Address is a plain entity
        Assertions.assertTrue(customer.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));
        // @AggregateRoot is meta-annotated with @Entity: never both
        Assertions.assertFalse(customer.contains("@org.jmolecules.ddd.annotation.Entity"));
        Assertions.assertTrue(address.contains("@org.jmolecules.ddd.annotation.Entity"));
        Assertions.assertFalse(address.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));

        // the synthetic id field, which is injected by the template and is not a ZDL field
        Assertions.assertTrue(customer.contains("@org.jmolecules.ddd.annotation.Identity"));

        // same JavaZdlModel.Entity as Customer.java, but a different artifact
        Assertions.assertTrue(repository.contains("@org.jmolecules.ddd.annotation.Repository"));
        Assertions.assertFalse(repository.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));
        Assertions.assertTrue(repository.contains("@org.jmolecules.architecture.hexagonal.SecondaryPort"));

        // same JavaZdlModel.Service, split across two artifacts
        Assertions.assertTrue(servicePort.contains("@org.jmolecules.architecture.hexagonal.PrimaryPort"));
        Assertions.assertFalse(servicePort.contains("hexagonal.Application"));
        Assertions.assertTrue(serviceImpl.contains("@org.jmolecules.architecture.hexagonal.Application"));
        Assertions.assertFalse(serviceImpl.contains("hexagonal.PrimaryPort"));

        // ZenWave services are application services, never jMolecules domain services
        Assertions.assertFalse(servicePort.contains("@org.jmolecules.ddd.annotation.Service"));
    }

    @Test
    public void jmolecules_annotations_never_leak_into_generated_tests() throws Exception {
        String targetFolder = "target/zdl/test_annotators_jmolecules_tests";
        new MainGenerator().generate(plugin(targetFolder)
                .withOption("useJMolecules", true));

        var repositoryTest = read(targetFolder,
                "src/test/java/io/zenwave360/example/core/application/CustomerServiceTest.java");
        Assertions.assertFalse(repositoryTest.contains("jmolecules"));
    }

    @Test
    public void only_aggregate_roots_get_a_repository_annotation() throws Exception {
        String targetFolder = "target/zdl/test_annotators_repository_scope";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        // Address is a plain entity: no repository is generated for it, so nothing should carry
        // @Repository on its behalf
        var address = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Address.java");
        Assertions.assertFalse(address.contains("@org.jmolecules.ddd.annotation.Repository"));

        var repository = read(targetFolder, "src/main/java/io/zenwave360/example/core/outbound/jpa/CustomerRepository.java");
        Assertions.assertTrue(repository.contains("@org.jmolecules.ddd.annotation.Repository"));
    }

    @Test
    public void asyncapi_events_are_not_domain_events() throws Exception {
        String targetFolder = "target/zdl/test_annotators_asyncapi_events";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        // every event in customer-address-relational.zdl is @asyncapi, so none is a domain event
        var domainEvents = new File(targetFolder, "src/main/java/io/zenwave360/example/core/domain/events");
        if (domainEvents.exists()) {
            for (File event : domainEvents.listFiles()) {
                var content = new String(Files.readAllBytes(event.toPath()));
                Assertions.assertFalse(content.contains("DomainEvent"),
                        event.getName() + " is an @asyncapi payload, not a domain event");
            }
        }
    }

    @Test
    public void plain_events_are_domain_events() throws Exception {
        String targetFolder = "target/zdl/test_annotators_domain_events";
        new MainGenerator().generate(domainEventsPlugin(targetFolder)
                .withOption("useJMolecules", true)
                .withOption("includeEmitEventsImplementation", true));

        var event = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/events/CustomerCreated.java");
        Assertions.assertTrue(event.contains("@org.jmolecules.event.annotation.DomainEvent"));
    }

    @Test
    public void hexagonal_annotates_publisher_and_listener_artifacts() throws Exception {
        String targetFolder = "target/zdl/test_annotators_elementless";
        new MainGenerator().generate(domainEventsPlugin(targetFolder)
                .withOption("useJMolecules", true)
                .withOption("includeEmitEventsImplementation", true));

        // artifacts with no backing ZDL element, resolved through javaModel.artifactAnnotations
        var publisherPort = read(targetFolder, "src/main/java/io/zenwave360/example/core/outbound/events/EventPublisher.java");
        var publisherImpl = read(targetFolder, "src/main/java/io/zenwave360/example/infrastructure/events/DefaultEventPublisher.java");

        Assertions.assertTrue(publisherPort.contains("@org.jmolecules.architecture.hexagonal.SecondaryPort"));
        Assertions.assertTrue(publisherImpl.contains("@org.jmolecules.architecture.hexagonal.SecondaryAdapter"));
    }

    @Test
    public void repository_port_is_a_secondary_port() throws Exception {
        String targetFolder = "target/zdl/test_annotators_repo_port";
        new MainGenerator().generate(plugin(targetFolder)
                .withOption("useJMolecules", true));

        var repository = read(targetFolder, "src/main/java/io/zenwave360/example/core/outbound/jpa/CustomerRepository.java");
        // both, from the same annotate(Entity, ..) call: the DDD stereotype and the hexagonal role
        Assertions.assertTrue(repository.contains("@org.jmolecules.ddd.annotation.Repository"));
        Assertions.assertTrue(repository.contains("@org.jmolecules.architecture.hexagonal.SecondaryPort"));
    }

    @Test
    public void input_dtos_are_value_objects() throws Exception {
        String targetFolder = "target/zdl/test_annotators_dtos";
        new MainGenerator().generate(plugin(targetFolder)
                .withOption("useJMolecules", true));

        var input = read(targetFolder, "src/main/java/io/zenwave360/example/core/inbound/dtos/CustomerInput.java");
        Assertions.assertTrue(input.contains("@org.jmolecules.ddd.annotation.ValueObject"));
    }

    @Test
    public void modulith_package_info_is_a_module() throws Exception {
        String targetFolder = "target/zdl/test_annotators_module";
        new MainGenerator().generate(plugin(targetFolder)
                .withOption("useJMolecules", true)
                .withOption("useSpringModulith", true)
                .withOption("layout.moduleBasePackage", "io.zenwave360.example.customer"));

        var packageInfo = read(targetFolder, "src/main/java/io/zenwave360/example/customer/package-info.java");
        Assertions.assertTrue(packageInfo.contains("@org.jmolecules.ddd.annotation.Module"));
        // the annotation must land before the package statement to be a package annotation
        Assertions.assertTrue(packageInfo.indexOf("@org.jmolecules.ddd.annotation.Module") < packageInfo.indexOf("package "));
    }

    @Test
    public void layered_architecture_uses_the_layered_vocabulary() throws Exception {
        String targetFolder = "target/zdl/test_annotators_layered";
        new MainGenerator().generate(plugin(targetFolder)
                .withLayout("LayeredProjectLayout")
                .withOption("useJMolecules", true));

        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/domain/Customer.java");
        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/service/CustomerService.java");

        Assertions.assertTrue(customer.contains("@org.jmolecules.architecture.layered.DomainLayer"));
        Assertions.assertTrue(servicePort.contains("@org.jmolecules.architecture.layered.ApplicationLayer"));
        // hexagonal vocabulary must not leak into a layered project
        Assertions.assertFalse(servicePort.contains("hexagonal"));
    }

    @Test
    public void jmolecules_architecture_test_is_generated_and_matches_the_architecture() throws Exception {
        String targetFolder = "target/zdl/test_annotators_archunit";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        var test = read(targetFolder, "src/test/java/io/zenwave360/example/JMoleculesArchitectureTest.java");
        Assertions.assertTrue(test.contains("JMoleculesDddRules.annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier"));
        // the analysed package must be resolved, not the empty string
        Assertions.assertTrue(test.contains("@AnalyzeClasses(packages = \"io.zenwave360.example\""));
        // default layout derives HEXAGONAL, so the hexagonal rule must be there and layered must not
        Assertions.assertTrue(test.contains("ensureHexagonal(VerificationDepth.STRICT)"));
        Assertions.assertFalse(test.contains("ensureLayering"));
        // shipped enabled; projects opt out by editing the generated file, which is never overwritten
        Assertions.assertTrue(test.contains("@ArchTest"));
        Assertions.assertFalse(test.contains("// @ArchTest"));
    }

    @Test
    public void architecture_tests_analyse_the_module_package() throws Exception {
        String targetFolder = "target/zdl/test_annotators_archunit_scope";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        // a bare {{moduleBasePackage}} renders empty, which silently analyses nothing
        var architectureTest = read(targetFolder, "src/test/java/io/zenwave360/example/ArchitectureTest.java");
        Assertions.assertTrue(architectureTest.contains("@AnalyzeClasses(packages = \"io.zenwave360.example\""));
        Assertions.assertTrue(architectureTest.contains("consideringOnlyDependenciesInAnyPackage(\"io.zenwave360.example..\")"));
    }

    @Test
    public void jmolecules_architecture_test_is_not_generated_when_jmolecules_is_off() throws Exception {
        String targetFolder = "target/zdl/test_annotators_archunit_off";
        new MainGenerator().generate(plugin(targetFolder));

        var test = new File(targetFolder, "src/test/java/io/zenwave360/example/JMoleculesArchitectureTest.java");
        Assertions.assertFalse(test.exists(), "must not be generated without useJMolecules");
    }

    @Test
    public void layered_annotates_the_three_tiers_truthfully() throws Exception {
        String targetFolder = "target/zdl/test_annotators_layered_events";
        new MainGenerator().generate(domainEventsPlugin(targetFolder)
                .withLayout("LayeredProjectLayout")
                .withOption("useJMolecules", true)
                .withOption("includeEmitEventsImplementation", true));

        // the layered layout is web -> service -> repository: the repository is the persistence tier
        var repository = read(targetFolder, "src/main/java/io/zenwave360/example/repository/jpa/CustomerRepository.java");
        Assertions.assertTrue(repository.contains("@org.jmolecules.architecture.layered.InfrastructureLayer"));

        var publisher = read(targetFolder, "src/main/java/io/zenwave360/example/events/DefaultEventPublisher.java");
        Assertions.assertTrue(publisher.contains("@org.jmolecules.architecture.layered.InfrastructureLayer"));

        // ensureLayering() encodes Evans' layering, which this architecture is not, so it is omitted.
        // Assert on the rule field, not the method name: the template explains the omission in a
        // comment that mentions the method.
        var archTest = read(targetFolder, "src/test/java/io/zenwave360/example/JMoleculesArchitectureTest.java");
        Assertions.assertFalse(archTest.contains("respectsLayeredArchitecture"));
        Assertions.assertFalse(archTest.contains("import org.jmolecules.archunit.JMoleculesArchitectureRules"));
    }

    @Test
    public void architecture_is_derived_from_the_layout() throws Exception {
        String targetFolder = "target/zdl/test_annotators_architecture_derived";
        // the default layout is hexagonal, so the vocabulary follows from it
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/core/inbound/CustomerService.java");
        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Customer.java");

        Assertions.assertTrue(customer.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));
        Assertions.assertTrue(servicePort.contains("@org.jmolecules.architecture.hexagonal.PrimaryPort"));
        Assertions.assertFalse(servicePort.contains("layered"));
    }

    @Test
    public void simple_domain_layout_derives_no_architecture() throws Exception {
        String targetFolder = "target/zdl/test_annotators_architecture_simple";
        new MainGenerator().generate(plugin(targetFolder)
                .withLayout("SimpleDomainProjectLayout")
                .withOption("useJMolecules", true));

        // SimpleDomain has no port boundary at all: outboundPackage IS the base package
        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/CustomerService.java");
        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/domain/Customer.java");

        Assertions.assertTrue(customer.contains("@org.jmolecules.ddd.annotation.AggregateRoot"));
        Assertions.assertFalse(servicePort.contains("hexagonal"));
        Assertions.assertFalse(servicePort.contains("layered"));
    }

    @Test
    public void jspecify_annotations_are_artifact_independent() throws Exception {
        String targetFolder = "target/zdl/test_annotators_jspecify";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJSpecify", true));

        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/core/inbound/CustomerService.java");
        var serviceImpl = read(targetFolder, "src/main/java/io/zenwave360/example/core/application/CustomerServiceImpl.java");

        // unscoped annotations render in every artifact that renders the element
        Assertions.assertTrue(servicePort.contains("@org.jspecify.annotations.NullMarked"));
        Assertions.assertTrue(serviceImpl.contains("@org.jspecify.annotations.NullMarked"));

        // this model has no optional method parameters (Customer? is an optional return type, not a
        // parameter), so @Nullable must not appear anywhere
        Assertions.assertFalse(servicePort.contains("@org.jspecify.annotations.Nullable"));
    }

    @Test
    public void identity_is_on_id_and_not_on_version() throws Exception {
        String targetFolder = "target/zdl/test_annotators_identity";
        new MainGenerator().generate(plugin(targetFolder).withOption("useJMolecules", true));

        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Customer.java");
        var identity = customer.indexOf("@org.jmolecules.ddd.annotation.Identity");
        var id = customer.indexOf("private Long id;");
        var version = customer.indexOf("private Integer version;");

        Assertions.assertTrue(identity > 0 && id > identity, "@Identity must immediately precede the id field");
        Assertions.assertTrue(version > id, "sanity: version follows id");
        // the only @Identity in the file is the one before id
        Assertions.assertEquals(identity, customer.lastIndexOf("@org.jmolecules.ddd.annotation.Identity"));
    }

    @Test
    public void no_annotations_are_added_by_default() throws Exception {
        String targetFolder = "target/zdl/test_annotators_disabled";
        new MainGenerator().generate(plugin(targetFolder));

        var customer = read(targetFolder, "src/main/java/io/zenwave360/example/core/domain/Customer.java");
        var servicePort = read(targetFolder, "src/main/java/io/zenwave360/example/core/inbound/CustomerService.java");

        Assertions.assertFalse(customer.contains("jmolecules"));
        Assertions.assertFalse(servicePort.contains("jmolecules"));
        Assertions.assertFalse(servicePort.contains("jspecify"));
    }
}
