package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AsyncAPIOpsTerraformKafkaTest {

    static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    static final String ASYNCAPI_CLIENT   = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-client.yml";
    static final String ASYNCAPI_COLLISION_ALPHA = "classpath:collision/alpha/asyncapi.yml";
    static final String ASYNCAPI_COLLISION_BETA  = "classpath:collision/beta/asyncapi.yml";

    @Test
    public void test_provider_generation() throws Exception {
        String targetFolder = "target/out/test_provider_generation";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PROVIDER)
                .withOption("server", "staging")
                .withOption("templates", "TerraformKafka")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        // Full address in resource name
        Assertions.assertTrue(topics.contains("merchandising_inventory_inventory_adjustment_reserve_stock_command_avro_v0\""));
        // Actual topic address in name field
        Assertions.assertTrue(topics.contains("merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0"));
        Assertions.assertTrue(topics.contains("partitions         = 3"), "Staging override");
        Assertions.assertTrue(topics.contains("coalesce(var.default_replication_factor, -1)") || topics.contains("replication_factor = 2"), "Template should support Terraform fallback or explicit value");
        Assertions.assertTrue(topics.contains("merge(var.default_topic_config"), "Topic config should merge Terraform defaults with AsyncAPI config");
        Assertions.assertTrue(topics.contains(".__."),"Error topics use .__.  separator");
        Assertions.assertTrue(topics.contains("retry"));
        Assertions.assertTrue(topics.contains("dlq"));
        Assertions.assertTrue(topics.contains("retention.ms"), "Error topics should have config block");
        // No resources for external topics
        Assertions.assertFalse(topics.contains("replenish_stock"), "External channel must not appear in topics.tf");

        String versions = Files.readString(Path.of(targetFolder + "/versions.tf"));
        Assertions.assertTrue(versions.contains("variable \"default_partitions\""));
        Assertions.assertTrue(versions.contains("variable \"default_replication_factor\""));
        Assertions.assertTrue(versions.contains("variable \"default_topic_config\""));

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertTrue(schemas.contains("ReserveStockCommand-value"));
        Assertions.assertTrue(schemas.contains("asyncapi/avro/ReserveStockCommand.avsc"));
        Assertions.assertTrue(schemas.contains("BACKWARD") || schemas.contains("FORWARD"));
        // No schemas for external topics
        Assertions.assertFalse(schemas.contains("replenish"), "External channel must not appear in schemas.tf");
        Assertions.assertTrue(new File(targetFolder + "/asyncapi/avro/ReserveStockCommand.avsc").exists());

        String acls = Files.readString(Path.of(targetFolder + "/acls.tf"));
        Assertions.assertTrue(acls.contains("User:merchandising.inventory.inventory-adjustment"));
        Assertions.assertTrue(acls.contains("Read") && acls.contains("Write"));
        Assertions.assertTrue(acls.contains("Describe"));
    }

    @Test
    public void test_provider_and_client_terraform() throws Exception {
        String targetFolder = "target/out/test_provider_and_client_terraform";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PROVIDER)
                .withOption("apiFiles", List.of(ASYNCAPI_CLIENT))
                .withOption("server", "staging")
                .withOption("templates", "TerraformKafka")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        Assertions.assertTrue(topics.contains("merchandising_inventory_inventory_adjustment_reserve_stock_command_avro_v0\""));
        Assertions.assertFalse(topics.contains("replenish_stock_command\""), "External channel must not get a kafka_topic resource");

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertFalse(schemas.contains("replenish"), "External channel must not get a schema resource");

        Assertions.assertTrue(new File(targetFolder + "/acls.tf").exists());
    }

    @Test
    public void test_provider_is_not_generated_twice_when_present_in_apiFile_and_apiFiles() throws Exception {
        String targetFolder = "target/out/test_provider_and_client_terraform_deduplicated";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PROVIDER)
                .withOption("apiFiles", List.of(ASYNCAPI_PROVIDER, ASYNCAPI_CLIENT))
                .withOption("server", "staging")
                .withOption("templates", "TerraformKafka")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        Assertions.assertEquals(1, occurrencesOf(topics, "resource \"kafka_topic\" \"merchandising_inventory_inventory_adjustment_reserve_stock_command_avro_v0\""));

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertEquals(1, occurrencesOf(schemas, "ReserveStockCommand-value"));
    }

    @Test
    public void test_missing_topic_settings_render_provider_aware_fallbacks() throws Exception {
        String targetFolder = "target/out/test_provider_generation_with_missing_topic_settings";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile("classpath:defaulting/asyncapi-minimal.yml")
                .withOption("templates", "TerraformKafka")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = Files.readString(Path.of(targetFolder + "/topics.tf"));
        Assertions.assertTrue(topics.contains("partitions         = var.default_partitions"));
        Assertions.assertTrue(topics.contains("replication_factor = coalesce(var.default_replication_factor, -1)"));
        Assertions.assertTrue(topics.contains("config = length(var.default_topic_config) > 0 ? var.default_topic_config : null"));
    }

    @Test
    public void test_provider_generation_bundles_only_required_imports() throws Exception {
        String targetFolder = "target/out/test_provider_generation_with_bundled_imports";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile("classpath:asyncapi-bundling/asyncapi.yml")
                .withOption("templates", "TerraformKafka")
                .withOption("avroImports", "classpath:asyncapi-bundling/imports")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertTrue(schemas.contains("${path.module}/asyncapi/avro/ShoppingCartItemAdded.avsc"));

        String bundledSchema = Files.readString(Path.of(targetFolder + "/asyncapi/avro/ShoppingCartItemAdded.avsc"));
        Assertions.assertTrue(bundledSchema.contains("\"name\":\"ShoppingCartItemAdded\""));
        Assertions.assertTrue(bundledSchema.contains("\"name\":\"ShoppingCart\""));
        Assertions.assertTrue(bundledSchema.contains("\"name\":\"Item\""));
        Assertions.assertFalse(bundledSchema.contains("UnusedImport"));
    }

    @Test
    public void test_multiple_specs_with_same_basename_generate_distinct_schema_output_folders() throws Exception {
        String targetFolder = "target/out/test_provider_generation_with_colliding_spec_names";
        new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_COLLISION_ALPHA)
                .withOption("apiFiles", List.of(ASYNCAPI_COLLISION_BETA))
                .withOption("templates", "TerraformKafka")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        Assertions.assertTrue(new File(targetFolder + "/asyncapi/avro/CustomerCreated.avsc").exists());
        Assertions.assertTrue(new File(targetFolder + "/asyncapi_2/avro/CustomerUpdated.avsc").exists());

        String schemas = Files.readString(Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertTrue(schemas.contains("${path.module}/asyncapi/avro/CustomerCreated.avsc"));
        Assertions.assertTrue(schemas.contains("${path.module}/asyncapi_2/avro/CustomerUpdated.avsc"));
    }

    private int occurrencesOf(String content, String token) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
