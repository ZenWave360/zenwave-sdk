package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AsyncApiAvroGenerator extends AbstractAsyncapiGenerator {
    private Logger log = LoggerFactory.getLogger(getClass());

    public String sourceProperty = "api";

    @DocumentedOption(description = "Message names to include in code generation (combined with operationIds). Generates code for ALL if left empty")
    public List<String> messageNames = new ArrayList<>();

    @DocumentedOption(description = "Avro Compiler Properties")
    public AvroCompilerProperties avroCompilerProperties = new AvroCompilerProperties();

    @DocumentedOption(description = "Target folder to generate code to.")
    public File targetFolder;

    @DocumentedOption(description = "Source folder inside folder to generate code to.")
    public String sourceFolder = "src/main/java";

    public Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(sourceProperty);
    }

    @Override
    protected Templates configureTemplates() {
        return null;
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);

        List<Map<String, Object>> avroImports = (List) contextModel.get(AvroSchemaLoader.AVRO_SCHEMAS_LIST);
        var avroMessages = AsyncAPIUtils.extractMessages(apiModel, AsyncApiProcessor.SchemaFormatType::isAvroFormat, operationIds, messageNames);
        var avroSchemas = avroMessages.stream().map(message -> {
            return (Map<String, Object>) JSONPath.getFirst(message, "$.payload.schema", "$.payload");
        }).toList();

        var allAvroSchemas = new ArrayList<>(avroSchemas);

        // Create a set of existing record identifiers (name + namespace)
        Set<String> existingRecords = avroSchemas.stream()
            .map(schema -> {
                String name = (String) schema.get("name");
                String namespace = (String) schema.get("namespace");
                return (namespace != null ? namespace + "." : "") + name;
            })
            .collect(Collectors.toSet());

        // Add imports only if they don't already exist
        avroImports.stream()
            .filter(schema -> {
                String name = (String) schema.get("name");
                String namespace = (String) schema.get("namespace");
                String identifier = (namespace != null ? namespace + "." : "") + name;
                return !existingRecords.contains(identifier);
            })
            .forEach(allAvroSchemas::add);

        try {
            new File(targetFolder, sourceFolder).mkdirs();
            AvroSchemaGenerator avroSchemaGenerator = new AvroSchemaGenerator();
            avroSchemaGenerator.avroCompilerProperties = avroCompilerProperties;
            avroSchemaGenerator.targetFolder = targetFolder;
            avroSchemaGenerator.sourceFolder = sourceFolder;

            return avroSchemaGenerator.generateJavaFromAvroSchemas(allAvroSchemas);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
