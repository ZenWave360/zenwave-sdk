package io.zenwave360.sdk.plugins;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.templating.TemplateInput;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

class BackendApplicationAsyncApiAdaptersGeneratorTest {

    @Test
    void implementsSeparatelyGeneratedAsyncApiContractWithRawPayloadAndTypedHeaders(@TempDir Path targetFolder)
            throws Exception {
        Plugin plugin = plugin(targetFolder, true);
        new MainGenerator().generate(plugin);

        Path packageFolder = targetFolder.resolve(
                "src/main/java/io/example/customer/adapters/events/customer");
        Path mapperContract = packageFolder.resolve("EventsMapper.java");
        Path mapStructExtension = packageFolder.resolve("EventsMapStructMapper.java");
        Path adapterImplementation = packageFolder.resolve("CreateCustomerChannelConsumerService.java");

        String mapperSource = Files.readString(mapperContract).replaceAll("\\s+", " ");
        Assertions.assertTrue(mapperSource.contains(
                "io.example.customer.core.inbound.dtos.CustomerInput createCustomerInput(io.example.customer.contract.model.Customer event)"));
        Assertions.assertFalse(mapperSource.contains("@Mapper"));
        Assertions.assertTrue(Files.readString(mapStructExtension)
                .contains("@Mapper"));
        Assertions.assertFalse(Files.readString(mapStructExtension)
                .contains("componentModel"));

        Assertions.assertFalse(Files.exists(
                packageFolder.resolve("GeneratedCreateCustomerChannelConsumerServiceBase.java")));
        String adapterSource = Files.readString(adapterImplementation).replaceAll("\\s+", " ");
        Assertions.assertTrue(adapterSource.contains(
                "implements io.example.customer.contract.consumer.ICreateCustomerChannelConsumerService"));
        Assertions.assertTrue(adapterSource.contains(
                "public void doCreateCustomer(io.example.customer.contract.model.Customer payload, io.example.customer.contract.consumer.ICreateCustomerChannelConsumerService.CustomerHeaders headers)"));
        Assertions.assertTrue(adapterSource.contains("@Component"));
        Assertions.assertTrue(adapterSource.contains(
                "private final io.example.customer.core.inbound.CustomerService customerService"));
        Assertions.assertTrue(mapperSource.contains(
                "EventsMapper INSTANCE = Mappers.getMapper(EventsMapStructMapper.class)"));
        Assertions.assertTrue(adapterSource.contains(
                "private final EventsMapper eventsMapper = EventsMapper.INSTANCE"));
        Assertions.assertTrue(adapterSource.contains(
                "public CreateCustomerChannelConsumerService(io.example.customer.core.inbound.CustomerService customerService)"));
        Assertions.assertTrue(adapterSource.contains(
                "customerService.createCustomer(eventsMapper.createCustomerInput(payload))"));

        Files.writeString(mapStructExtension, Files.readString(mapStructExtension) + "\n// developer customization\n");
        Files.writeString(adapterImplementation, Files.readString(adapterImplementation) + "\n// developer customization\n");
        new MainGenerator().generate(plugin);
        Assertions.assertTrue(Files.readString(mapStructExtension).contains("// developer customization"));
        Assertions.assertTrue(Files.readString(adapterImplementation).contains("// developer customization"));
    }

    @Test
    void customRequiredAdapterStillInjectsMatchedZdlService(@TempDir Path targetFolder) throws Exception {
        Plugin plugin = plugin(targetFolder, true, "classpath:zdl/asyncapi-consumer-adapter-custom.zdl");
        new MainGenerator().generate(plugin);

        Path adapterImplementation = targetFolder.resolve(
                "src/main/java/io/example/customer/adapters/events/customer/CreateCustomerChannelConsumerService.java");
        String adapterSource = Files.readString(adapterImplementation).replaceAll("\\s+", " ");

        Assertions.assertTrue(adapterSource.contains(
                "private final io.example.customer.core.inbound.CustomerService customerService"));
        Assertions.assertTrue(adapterSource.contains(
                "public CreateCustomerChannelConsumerService(io.example.customer.core.inbound.CustomerService customerService)"));
        Assertions.assertTrue(adapterSource.contains("this.customerService = customerService"));
        Assertions.assertFalse(adapterSource.contains("EventsMapper"));
        Assertions.assertTrue(adapterSource.contains("TODO CUSTOM_REQUIRED"));
    }

    @Test
    void customProjectTemplatesAlsoControlAsyncApiAdapters(@TempDir Path targetFolder) throws Exception {
        Plugin plugin = plugin(targetFolder, true)
                .withOption("templates", "new " + CustomBackendProjectTemplates.class.getName());
        new MainGenerator().generate(plugin);

        Path modulePackage = targetFolder.resolve("src/main/java/io/example/customer");
        Path adapterPackage = modulePackage.resolve("adapters/events/customer");
        Path customAdapter = adapterPackage.resolve(
                "custom/CustomizedCreateCustomerChannelConsumerService.java");
        Path backendMarker = modulePackage.resolve("CustomizedBackendMarker.java");

        Assertions.assertTrue(Files.exists(backendMarker),
                "The primary backend pass should use the selected ProjectTemplates class");
        Assertions.assertTrue(Files.readString(backendMarker)
                .contains("class CustomizedBackendMarker"));
        Assertions.assertTrue(Files.exists(customAdapter),
                "The AsyncAPI pass should use a separately configured instance of that same class");
        Assertions.assertTrue(Files.readString(customAdapter)
                .contains("package io.example.customer.adapters.events.customer.custom;"));
        Assertions.assertFalse(Files.exists(
                adapterPackage.resolve("CreateCustomerChannelConsumerService.java")));
    }

    @Test
    void placesAsyncApiAdaptersInTheConfiguredCoreImplementationModule(@TempDir Path targetFolder)
            throws Exception {
        Plugin plugin = plugin(targetFolder, true)
                .withOption("mavenModulesPrefix", "customer");
        new MainGenerator().generate(plugin);

        Path adapter = targetFolder.resolve(
                "customer-core-impl/src/main/java/io/example/customer/adapters/events/customer/"
                        + "CreateCustomerChannelConsumerService.java");
        Assertions.assertTrue(Files.exists(adapter));
    }

    @Test
    void eventListenerImplementationsAreOptInByDefault(@TempDir Path targetFolder)
            throws Exception {
        new MainGenerator().generate(plugin(targetFolder, null));

        Path packageFolder = targetFolder.resolve(
                "src/main/java/io/example/customer/adapters/events/customer");
        Assertions.assertFalse(Files.exists(packageFolder.resolve("CreateCustomerChannelConsumerService.java")));
    }

    private Plugin plugin(Path targetFolder, Boolean implementEventListeners) {
        return plugin(targetFolder, implementEventListeners, "classpath:zdl/asyncapi-consumer-adapter.zdl");
    }

    private Plugin plugin(Path targetFolder, Boolean implementEventListeners, String zdlFile) {
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(zdlFile)
                .withTargetFolder(targetFolder.toString())
                .withOption("basePackage", "io.example.customer")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("skipFormatting", true);
        if (implementEventListeners != null) {
            plugin.withOption("implementEventListeners", implementEventListeners);
        }
        return plugin;
    }

    public static class CustomBackendProjectTemplates extends BackendApplicationProjectTemplates {

        @DocumentedOption(description = "Prefix used by the custom backend architecture templates")
        public String customAdapterClassPrefix = "Customized";

        public CustomBackendProjectTemplates() {
            singleTemplates.add(new TemplateInput(
                    joinPath(getTemplatesFolder(), "src/main/java", "custom/BackendMarker.java"),
                    "src/main/java/{{asPackageFolder layout.moduleBasePackage}}/"
                            + "{{customAdapterClassPrefix}}BackendMarker.java",
                    JAVA));
            asyncApiAdapterByChannelTemplates.clear();
            asyncApiAdapterByChannelTemplates.add(new TemplateInput(
                    joinPath(getTemplatesFolder(), "src/main/java",
                            "adapters/events/asyncapi/imperative/CustomConsumerService.java"),
                    "{{asyncapiAdaptersModulePrefix}}src/main/java/"
                            + "{{asPackageFolder layout.adaptersEventsPackage}}/custom/"
                            + "{{customAdapterClassPrefix}}{{consumerServiceName}}.java",
                    JAVA).withSkipOverwrite(true));
        }
    }
}
