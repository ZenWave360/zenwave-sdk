package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AvroSchemaGeneratorTest {

    @BeforeEach
    public void setup() throws IOException {
        FileUtils.deleteDirectory(new File("target/zenwave630/avro-out"));
    }

    @Test
    public void test_avro_schema_generator() throws Exception {
        Plugin plugin = new AvroSchemaGeneratorPlugin()
                .withTargetFolder("target/zenwave630/customer-event")
                .withOption("avroCompilerProperties.sourceDirectory", "src/test/resources/avros/customer-event")
                .withOption("avroCompilerProperties.stringType", "String")
                ;

        new MainGenerator().generate(plugin);

        // Verify generated Java classes exist
        Assertions.assertTrue(new File("target/zenwave630/customer-event/io/zenwave360/example/core/outbound/events/dtos/Address.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/customer-event/io/zenwave360/example/core/outbound/events/dtos/CustomerEvent.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/customer-event/io/zenwave360/example/core/outbound/events/dtos/PaymentMethod.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/customer-event/io/zenwave360/example/core/outbound/events/dtos/PaymentMethodType.java").exists());
    }

    @Test
    public void test_avro_schema_generator_single_file() throws Exception {
        Plugin plugin = new AvroSchemaGeneratorPlugin()
                .withTargetFolder("target/zenwave630/customer-event-single")
                .withOption("avroCompilerProperties.imports", List.of("src/test/resources/avros/customer-event/Address.avsc"))
                ;

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/customer-event-single/io/zenwave360/example/core/outbound/events/dtos/Address.java").exists());
    }

    @Test
    public void test_avro_schema_generator_array() throws Exception {
        Plugin plugin = new AvroSchemaGeneratorPlugin()
                .withTargetFolder("target/zenwave630/avros-array")
                .withOption("avroCompilerProperties.imports", List.of("src/test/resources/avros/array.avsc"))
                ;

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/avros-array/io/zenwave360/example/core/outbound/events/dtos/Address.java").exists());
    }

    @Test
    public void test_avro_schema_generator_with_custom_logical_types() throws Exception {
        Plugin plugin = new AvroSchemaGeneratorPlugin()
                .withOption("targetFolder","target/zenwave630/avros-custom-logical-types")
                .withOption("avroCompilerProperties.imports", List.of("src/test/resources/avros/customer-event"))
                .withOption("avroCompilerProperties.customLogicalTypeFactories",
                        String.join(",", List.of(CustomLogicalTypeFactory1.class.getName(), CustomLogicalTypeFactory2.class.getName())))
                .withOption("avroCompilerProperties.customConversions", Conversion1.class.getName())
                ;

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/avros-custom-logical-types/io/zenwave360/example/core/outbound/events/dtos/Address.java").exists());
    }

    public static class CustomLogicalTypeFactory1 implements LogicalTypes.LogicalTypeFactory {
        @Override
        public LogicalType fromSchema(Schema schema) {
            return null;
        }

//        @Override
        public String getTypeName() {
            return this.getClass().getName();
        }
    }

    public static class CustomLogicalTypeFactory2 implements LogicalTypes.LogicalTypeFactory {

        @Override
        public LogicalType fromSchema(Schema schema) {
            return null;
        }

//        @Override
        public String getTypeName() {
            return this.getClass().getName();
        }
    }

    public static class Conversion1 extends org.apache.avro.Conversion<String> {

        @Override
        public Class<String> getConvertedType() {
            return null;
        }

        @Override
        public String getLogicalTypeName() {
            return "";
        }
    }

}
