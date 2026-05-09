package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AvroSchemaGenerator extends Generator {

    private Logger log = LoggerFactory.getLogger(getClass());
    private final AvroSchemaLoader avroSchemaLoader = new AvroSchemaLoader();

    @DocumentedOption(description = "Avro Compiler Properties")
    public AvroCompilerProperties avroCompilerProperties = new AvroCompilerProperties();

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder = new File ("target/generated-sources/avro");

    @DocumentedOption(description = "Source folder inside folder to generate code to.")
    public String sourceFolder = "";

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        try {
            List<Map<String, Object>> avroSchemas = (List) contextModel.get(AvroSchemaLoader.AVRO_SCHEMAS_LIST);
            return generateJavaFromAvroSchemas(avroSchemas);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    protected GeneratedProjectFiles generateJavaFromAvroSchemas(List<Map<String, Object>> avroSchemas) throws IOException {
        avroSchemas = avroSchemaLoader.sortSchemas(avroSchemas);
        ObjectMapper mapper = new ObjectMapper();
        String avscJson = mapper.writeValueAsString(avroSchemas);

        Schema schema = null;
        try {
            log.debug("Parsing avsc files...");
            Schema.Parser parser = new Schema.Parser();
            schema = parser.parse(avscJson);
        } catch (Exception e) {
            log.error("Error parsing avsc files: {}", avscJson, e);
            throw e;
        }
        try {
            var targetSourceFolder = new File(targetFolder, sourceFolder);
            log.info("Generating avro classes to: {}", targetSourceFolder);
            SpecificCompiler compiler = new SpecificCompiler(schema);
            setCompilerProperties(compiler, avroCompilerProperties);
            compiler.compileToDestination(avroCompilerProperties.sourceDirectory, targetSourceFolder);
        } catch (Exception e) {
            log.error("Error generating avsc files", e);
            throw e;
        }

        return new GeneratedProjectFiles();
    }

    protected void setCompilerProperties(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setTemplateDir(properties.templateDirectory);
        compiler.setStringType(GenericData.StringType.valueOf(properties.stringType));
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(properties.fieldVisibility.toUpperCase()));
        compiler.setCreateSetters(properties.createSetters);
        compiler.setOutputCharacterEncoding(properties.outputCharacterEncoding);

        if (avroSchemaLoader.isAvroVersionLater("1.8.0")) {
            setCompilerProperties_v1_8_0(compiler, properties);
        }
        if (avroSchemaLoader.isAvroVersionLater("1.8.2")) {
            setCompilerProperties_v1_8_2(compiler, properties);
        }
        if (avroSchemaLoader.isAvroVersionLater("1.9.0")) {
            setCompilerProperties_v1_9_0(compiler, properties);
        }
        if (avroSchemaLoader.isAvroVersionLater("1.11.0")) {
            setCompilerProperties_v1_11_0(compiler, properties);
        }
        if (avroSchemaLoader.isAvroVersionLater("1.12.0")) {
            setCompilerProperties_v1_12_0(compiler, properties);
        }
    }

    protected void setCompilerProperties_v1_8_0(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setEnableDecimalLogicalType(properties.enableDecimalLogicalType);
    }

    protected void setCompilerProperties_v1_8_2(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setCreateOptionalGetters(properties.createOptionalGetters);
        compiler.setGettersReturnOptional(properties.gettersReturnOptional);
        if (properties.customConversions != null) {
            for (var conversionClass : properties.customConversions) {
                compiler.addCustomConversion(conversionClass);
            }
        }
    }

    protected void setCompilerProperties_v1_9_0(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setOptionalGettersForNullableFieldsOnly(properties.optionalGettersForNullableFieldsOnly);
        compiler.setAdditionalVelocityTools(properties.instantiateAdditionalVelocityTools());
    }

    protected void setCompilerProperties_v1_11_0(SpecificCompiler compiler, AvroCompilerProperties properties) {
        if (properties.customLogicalTypeFactories != null) {
            for (var logicalTypeFactoryClass : properties.customLogicalTypeFactories) {
                try {
                    LogicalTypes.LogicalTypeFactory factoryInstance =
                            logicalTypeFactoryClass.getDeclaredConstructor().newInstance();
                    LogicalTypes.register(factoryInstance);
                } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate logical type factory " + logicalTypeFactoryClass, e);
                }
            }
        }
    }

    protected void setCompilerProperties_v1_12_0(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setCreateNullSafeAnnotations(properties.createNullSafeAnnotations);
        compiler.setRecordSpecificClass(properties.recordSpecificClass);
        compiler.setErrorSpecificClass(properties.errorSpecificClass);
    }

    protected void setCompilerProperties_v1_12_1(SpecificCompiler compiler, AvroCompilerProperties properties) {
//        compiler.setNullSafeAnnotationNullable(properties.nullSafeAnnotationNullable);
//        compiler.setNullSafeAnnotationNotNull(properties.nullSafeAnnotationNotNull);
    }
}
