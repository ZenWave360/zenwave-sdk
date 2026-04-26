package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AsyncAPIOpsTerraformConfluentTest {

    static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    static final String ASYNCAPI_CLIENT   = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-client.yml";

    @Test
    public void test_provider_generation() throws Exception {
        String targetFolder = "target/out/test_confluent_provider_generation";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PROVIDER)
                .withOption("server", "staging")
                .withOption("templates", "TerraformConfluent")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String versions = Files.readString(Path.of(targetFolder + "/versions.tf"));
        Assertions.assertTrue(versions.contains("confluentinc/confluent"));
        Assertions.assertFalse(versions.contains("Mongey/kafka"));
        Assertions.assertFalse(versions.contains("cultureamp/schemaregistry"));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        Assertions.assertTrue(topics.contains("resource \"confluent_kafka_topic\""));
        Assertions.assertTrue(topics.contains("merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0"));
        Assertions.assertTrue(topics.contains("partitions_count = 3"), "Staging override");
        Assertions.assertFalse(topics.contains("replenish_stock"), "External channel must not appear in topics.tf");

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertTrue(schemas.contains("resource \"confluent_schema\""));
        Assertions.assertTrue(schemas.contains("resource \"confluent_subject_config\""));
        Assertions.assertTrue(schemas.contains("subject_name = \"ReserveStockCommand-value\""));
        Assertions.assertTrue(schemas.contains("format       = \"AVRO\""));
        Assertions.assertTrue(schemas.contains("compatibility_level"));
        Assertions.assertTrue(new File(targetFolder + "/asyncapi/avro/ReserveStockCommand.avsc").exists());

        String acls = Files.readString(Path.of(targetFolder + "/acls.tf"));
        Assertions.assertTrue(acls.contains("resource \"confluent_kafka_acl\""));
        Assertions.assertTrue(acls.contains("operation     = \"READ\""));
        Assertions.assertTrue(acls.contains("operation     = \"WRITE\""));
        Assertions.assertTrue(acls.contains("permission    = \"ALLOW\""));
    }

    @Test
    public void test_provider_and_client_terraform() throws Exception {
        String targetFolder = "target/out/test_provider_and_client_confluent";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PROVIDER)
                .withOption("apiFiles", List.of(ASYNCAPI_CLIENT))
                .withOption("server", "staging")
                .withOption("templates", "TerraformConfluent")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        Assertions.assertTrue(topics.contains("merchandising_inventory_inventory_adjustment_reserve_stock_command_avro_v0\""));
        Assertions.assertFalse(topics.contains("replenish_stock_command\""), "External channel must not get a topic resource");

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertFalse(schemas.contains("replenish"), "External channel must not get a schema resource");

        Assertions.assertTrue(new File(targetFolder + "/acls.tf").exists());
    }
}
