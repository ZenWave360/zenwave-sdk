package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalTypes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@DocumentedOption(description = "All Avro Compiler Properties to pass downstream to Avro Compiler", docLink = "https://github.com/ZenWave360/zenwave-sdk/blob/main/plugins/avro-schema-compiler/src/main/java/io/zenwave360/sdk/plugins/AvroCompilerProperties.java")
public class AvroCompilerProperties {

    @DocumentedOption(description = "Avro schema file or folder containing avro schemas")
    public File sourceDirectory;

    @DocumentedOption(description = "Avro schema files or folders containing avro schemas. It supports local files/folders, `classpath:` files/folders or `https://` file resources.")
    public List<String> imports;

    @DocumentedOption(description = "A set of Ant-like inclusion patterns used to select files from the source tree that are to be processed. By default, the pattern **\\/*.avsc is used to include all avro schema files.")
    public List<String> includes = List.of("**/*.avsc");

    @DocumentedOption(description = "A set of Ant-like exclusion patterns used to prevent certain files from being processed. By default, this set is empty such that no files are excluded.")
    public List<String> excludes;

    public String templateDirectory = "/org/apache/avro/compiler/specific/templates/java/classic/";
    public String stringType = "CharSequence";
    public String fieldVisibility = "PRIVATE";
    public boolean createOptionalGetters = false;
    public boolean gettersReturnOptional = false;
    public boolean optionalGettersForNullableFieldsOnly = false;
    public boolean createSetters = true;
    public boolean createNullSafeAnnotations = false;
    public String nullSafeAnnotationNullable = "org.jetbrains.annotations.Nullable";
    public String nullSafeAnnotationNotNull = "org.jetbrains.annotations.NotNull";
    public boolean enableDecimalLogicalType = false;
    public String outputCharacterEncoding = "UTF-8";
    public List<String> velocityToolsClassesNames = new ArrayList<>();
    public String recordSpecificClass = "org.apache.avro.specific.SpecificRecordBase";
    public String errorSpecificClass = "org.apache.avro.specific.SpecificExceptionBase";

    @DocumentedOption(description = "Custom Logical Type Factories")
    public List<Class<? extends LogicalTypes.LogicalTypeFactory>> customLogicalTypeFactories;

    @DocumentedOption(description = "Custom Conversions")
    public List<Class<? extends Conversion<?>>> customConversions;

    public List<Object> instantiateAdditionalVelocityTools() {
        final List<Object> velocityTools = new ArrayList<>(velocityToolsClassesNames.size());
        for (String velocityToolClassName : velocityToolsClassesNames) {
            try {
                Class<?> klass = Class.forName(velocityToolClassName);
                velocityTools.add(klass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return velocityTools;
    }
}
