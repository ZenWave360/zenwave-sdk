package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.apache.avro.Schema;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

class AsyncAPIOpsAvroBundler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AvroSchemaLoader loader;
    private final List<String> avroImports;

    AsyncAPIOpsAvroBundler(List<String> avroImports, ClassLoader projectClassLoader) {
        this.avroImports = avroImports != null ? avroImports : List.of();
        this.loader = new AvroSchemaLoader();
        this.loader.withProjectClassLoader(projectClassLoader);
    }

    TemplateOutput bundle(AsyncAPIOpsIntent.SchemaIntent schemaIntent) {
        try {
            URI rootSchemaUri = URI.create(schemaIntent.sourceSchemaUri);
            Map<String, Object> rootSchema = loadSingleSchema(rootSchemaUri);

            List<URI> allUris = new ArrayList<>();
            allUris.add(rootSchemaUri);
            allUris.addAll(loader.collectImportUris(avroImports));

            List<Map<String, Object>> sortedSchemas = loader.sortSchemas(loader.loadSchemas(distinct(allUris)));

            String rootFullName = getFullName(rootSchema);
            Schema.Parser parser = new Schema.Parser();
            parser.parse(mapper.writeValueAsString(sortedSchemas));
            Schema bundledSchema = parser.getTypes().get(rootFullName);
            if (bundledSchema == null) {
                throw new IllegalStateException("Could not locate bundled Avro schema for " + rootFullName);
            }

            return new TemplateOutput(schemaIntent.schemaFile, bundledSchema.toString(), OutputFormatType.JSON.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to bundle Avro schema: " + schemaIntent.sourceSchemaUri, e);
        }
    }

    private Map<String, Object> loadSingleSchema(URI schemaUri) throws IOException {
        List<Map<String, Object>> schemas = loader.loadSchemas(List.of(schemaUri));
        if (schemas.isEmpty()) {
            throw new IllegalStateException("No Avro schema loaded from " + schemaUri);
        }
        Object root = schemas.get(schemas.size() - 1);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalStateException("Expected a single Avro schema object in " + schemaUri);
        }
        return (Map<String, Object>) rootMap;
    }

    private String getFullName(Map<String, Object> schema) {
        String name = (String) schema.get("name");
        String namespace = (String) schema.get("namespace");
        return namespace != null && !namespace.isBlank() ? namespace + "." + name : name;
    }

    private List<URI> distinct(List<URI> uris) {
        return new ArrayList<>(new LinkedHashSet<>(uris));
    }
}
