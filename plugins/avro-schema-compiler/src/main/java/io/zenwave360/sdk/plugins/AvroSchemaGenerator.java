package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.utils.AntStyleMatcher;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AvroSchemaGenerator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "List of avro schema files to generate code for. It is alternative to sourceDirectory and imports.")
    public List<File> avroFiles;

    @DocumentedOption(description = "Avro Compiler Properties")
    public AvroCompilerProperties avroCompilerProperties = new AvroCompilerProperties();

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder = new File ("target/generated-sources/avro");

    @Override
    protected Templates configureTemplates() {
        return null;
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        try {
            if(avroFiles != null && !avroFiles.isEmpty()) {
                log.info("Using {} avro files: {}", avroFiles.size(), avroFiles);
            }
            else {
                avroFiles = collectAvscFiles(avroCompilerProperties.sourceDirectory, avroCompilerProperties.imports, avroCompilerProperties.includes, avroCompilerProperties.excludes);
                log.debug("Found {} avsc files: {}", avroFiles.size(), avroFiles);
            }
            String avscJson = asJsonArray(avroFiles);
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
                log.info("Generating avro classes to: {}", targetFolder);
                SpecificCompiler compiler = new SpecificCompiler(schema);
                setCompilerProperties(compiler, avroCompilerProperties);
                compiler.compileToDestination(avroCompilerProperties.sourceDirectory, targetFolder);
            } catch (Exception e) {
                log.error("Error generating avsc files", e);
                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new GeneratedProjectFiles();
    }


    private List<File> collectAvscFiles(File sourceFolder, List<String> imports, List<String> includes, List<String> excludes) throws IOException {
        Set<File> avscFiles = new HashSet<>();

        // Process sourceFolder if provided
        if (sourceFolder != null && sourceFolder.exists()) {
            if (sourceFolder.isDirectory()) {
                log.info("Collecting avsc files from source folder: {}", sourceFolder);
                avscFiles.addAll(Files.walk(sourceFolder.toPath())
                        .filter(Files::isRegularFile)
//                        .filter(p -> matchesIncludes(p, includes) && !matchesExcludes(p, excludes))
                        .map(Path::toFile)
                        .toList());
            } else if (sourceFolder.isFile() && sourceFolder.getName().endsWith(".avsc")) {
                avscFiles.add(sourceFolder);
            }
        }

        // Process imports if provided
        if (imports != null) {
            log.info("Collecting avsc files from imports: {}", imports);
            for (String importPath : imports) {
                Path path = Paths.get(importPath);
                if (Files.isDirectory(path)) {
                    avscFiles.addAll(Files.walk(path)
                            .filter(Files::isRegularFile)
//                            .filter(p -> matchesIncludes(p, includes) && !matchesExcludes(p, excludes))
                            .map(Path::toFile)
                            .toList());
                } else if (Files.isRegularFile(path) && importPath.endsWith(".avsc")) {
                    avscFiles.add(path.toFile());
                } else {
                    log.warn("Skipping invalid import path: {}", importPath);
                }
            }
        }

        return avscFiles.stream().toList();
    }

    private boolean matchesIncludes(Path path, List<String> includes) {
        if (includes == null || includes.isEmpty()) {
            boolean matches = path.toString().endsWith(".avsc");
            log.debug("File {} matches default include pattern: {}", path, matches);
            return matches;
        }
        var pathString = path.toString().replace("\\", "/");
        boolean matches = includes.stream().anyMatch(include -> AntStyleMatcher.match(include, pathString));
        log.debug("File {} matches includes {}: {}", pathString, includes, matches);
        return matches;
    }

    private boolean matchesExcludes(Path path, List<String> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            log.debug("File {} has no excludes, not excluded", path);
            return false;
        }
        var pathString = path.toString().replace("\\", "/");
        boolean matches = excludes.stream().anyMatch(exclude -> AntStyleMatcher.match(exclude, pathString));
        log.debug("File {} matches excludes {}: {}", pathString, excludes, matches);
        return matches;
    }

    private String asJsonArray(Collection<File> avscFiles) throws IOException {
        List<String> allSchemas = new ArrayList<>();

        for (File file : avscFiles) {
            String content = new String(Files.readAllBytes(file.toPath())).trim();

            if (content.startsWith("[") && content.endsWith("]")) {
                // File contains an array of schemas - remove outer brackets
                String innerContent = content.substring(1, content.length() - 1).trim();
                if (!innerContent.isEmpty()) {
                    allSchemas.add(innerContent);
                }
            } else {
                // File contains a single schema
                allSchemas.add(content);
            }
        }

        return "[" + String.join(",", allSchemas) + "]";
    }

    protected void setCompilerProperties(SpecificCompiler compiler, AvroCompilerProperties properties) {
        compiler.setTemplateDir(properties.templateDirectory);
        compiler.setStringType(GenericData.StringType.valueOf(properties.stringType));
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(properties.fieldVisibility.toUpperCase()));
        compiler.setCreateOptionalGetters(properties.createOptionalGetters);
        compiler.setGettersReturnOptional(properties.gettersReturnOptional);
        compiler.setOptionalGettersForNullableFieldsOnly(properties.optionalGettersForNullableFieldsOnly);
        compiler.setCreateSetters(properties.createSetters);
//        compiler.setCreateNullSafeAnnotations(properties.createNullSafeAnnotations);
//        compiler.setNullSafeAnnotationNullable(properties.nullSafeAnnotationNullable);
//        compiler.setNullSafeAnnotationNotNull(properties.nullSafeAnnotationNotNull);
        compiler.setEnableDecimalLogicalType(properties.enableDecimalLogicalType);
        compiler.setOutputCharacterEncoding(properties.outputCharacterEncoding);
        compiler.setAdditionalVelocityTools(properties.instantiateAdditionalVelocityTools());
//        compiler.setRecordSpecificClass(properties.recordSpecificClass);
//        compiler.setErrorSpecificClass(properties.errorSpecificClass);

        if (properties.customConversions != null) {
            for (var conversionClass : properties.customConversions) {
                compiler.addCustomConversion(conversionClass);
            }
        }
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

}
