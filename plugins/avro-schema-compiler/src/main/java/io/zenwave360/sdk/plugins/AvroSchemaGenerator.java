package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.utils.JSONPath;
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
import java.net.URI;
import java.util.*;

public class AvroSchemaGenerator extends Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

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
        avroSchemas = sortSchemas(avroSchemas);
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

    public List<Map<String, Object>> sortSchemas(List<Map<String, Object>> schemas) throws JsonProcessingException {
        if(isAvroVersionLater("1.12.0")) {
            return schemas;
        }
        log.info("Avro version detected {} < 1.12.0. Sorting schemas...", AVRO_VERSION);

        log.debug("Sorting schemas: {}", JSONPath.get(schemas, "$.[*].name", List.of()));
        schemas.sort(createDependencyComparator());
        log.debug("Sorted schemas: {}", JSONPath.get(schemas, "$.[*].name", List.of()));

        return schemas;
    }

    private Comparator<Map<String, Object>> createDependencyComparator() {
        return (map1, map2) -> {
            String name1 = (String) map1.get("name");
            String name2 = (String) map2.get("name");
            String type1 = (String) map1.get("type");
            String type2 = (String) map2.get("type");

            List<String> dependencies1 = extractDependencies(map1);
            List<String> dependencies2 = extractDependencies(map2);

            boolean map1IsEnum = "enum".equals(type1);
            boolean map2IsEnum = "enum".equals(type2);
            boolean map1HasNoDeps = dependencies1.isEmpty();
            boolean map2HasNoDeps = dependencies2.isEmpty();

            // Enums and maps with no dependencies go first
            if ((map1IsEnum || map1HasNoDeps) && !(map2IsEnum || map2HasNoDeps)) {
                return -1; // map1 comes before map2
            } else if (!(map1IsEnum || map1HasNoDeps) && (map2IsEnum || map2HasNoDeps)) {
                return 1; // map1 comes after map2
            }

            boolean map1DependsOnMap2 = dependencies1.contains(name2);
            boolean map2DependsOnMap1 = dependencies2.contains(name1);

            if (map1DependsOnMap2 && !map2DependsOnMap1) {
                return 1; // map1 comes after map2
            } else if (!map1DependsOnMap2 && map2DependsOnMap1) {
                return -1; // map1 comes before map2
            }
            return 0; // no dependency relationship
        };
    }

    private List<String> extractDependencies(Map<String, Object> schema) {
        List<Object> fieldTypes = JSONPath.get(schema, "$.fields[*].type", List.of());
        List<String> dependencies = new ArrayList<>();

        // Set of primitive Avro types that should not be considered dependencies
        Set<String> primitiveTypes = Set.of("null", "boolean", "int", "long", "float", "double", "bytes", "string");

        for (Object fieldType : fieldTypes) {
            if (fieldType instanceof String typeName) {
                if (!primitiveTypes.contains(typeName)) {
                    dependencies.add(typeName);
                }
            } else if (fieldType instanceof List) {
                // Handle union types like ["null", "string"] or ["null", "MyCustomType"]
                List<Object> unionTypes = (List<Object>) fieldType;
                for (Object unionType : unionTypes) {
                    if (unionType instanceof String) {
                        String typeName = (String) unionType;
                        if (!primitiveTypes.contains(typeName)) {
                            dependencies.add(typeName);
                        }
                    }
                }
            } else if (fieldType instanceof Map) {
                Map<String, Object> typeMap = (Map<String, Object>) fieldType;
                String items = JSONPath.get(typeMap, "$.items", null);
                if (items != null && !primitiveTypes.contains(items)) {
                    dependencies.add(items);
                }
            }
        }

        return dependencies;
    }

    protected void setCompilerProperties(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setTemplateDir(properties.templateDirectory);
        compiler.setStringType(GenericData.StringType.valueOf(properties.stringType));
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(properties.fieldVisibility.toUpperCase()));
        compiler.setCreateSetters(properties.createSetters);
        compiler.setOutputCharacterEncoding(properties.outputCharacterEncoding);

        if (isAvroVersionLater("1.8.0")) {
            setCompilerProperties_v1_8_0(compiler, properties);
        }
        if (isAvroVersionLater("1.8.2")) {
            setCompilerProperties_v1_8_2(compiler, properties);
        }
        if (isAvroVersionLater("1.9.0")) {
            setCompilerProperties_v1_9_0(compiler, properties);
        }
        if (isAvroVersionLater("1.11.0")) {
            setCompilerProperties_v1_11_0(compiler, properties);
        }
        if (isAvroVersionLater("1.12.0")) {
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

    private static String AVRO_VERSION = _getAvroVersion();
    private static String _getAvroVersion() {
        Package avroPackage = Schema.class.getPackage();
        if (avroPackage != null && avroPackage.getImplementationVersion() != null) {
            return avroPackage.getImplementationVersion();
        }
        return "0.0.0";
    }

    private boolean isAvroVersionLater(String version) {
        String[] currentVersionParts = AVRO_VERSION.split("\\.");
        String[] targetVersionParts = version.split("\\.");

        int currentMajor = Integer.parseInt(currentVersionParts[0]);
        int currentMinor = Integer.parseInt(currentVersionParts[1]);
        int targetMajor = Integer.parseInt(targetVersionParts[0]);
        int targetMinor = Integer.parseInt(targetVersionParts[1]);

        return currentMajor > targetMajor ||
                (currentMajor == targetMajor && currentMinor >= targetMinor);
    }


}
