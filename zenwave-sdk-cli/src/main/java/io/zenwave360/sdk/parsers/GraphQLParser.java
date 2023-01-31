package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import graphql.schema.idl.SchemaParser;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.utils.Maps;

public class GraphQLParser implements Parser {

    @DocumentedOption(description = "API Specification File")
    public URI specFile;
    public String targetProperty = "graphql";

    private ClassLoader projectClassLoader;

    @Override
    public GraphQLParser withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public Map<String, Object> parse() throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        var typeDefinitionRegistry = schemaParser.parse(loadSpecFile(specFile.toString()));
        var graphql = Maps.of(
                "typeDefinitionRegistry", typeDefinitionRegistry,
                "objectTypeExtensions", typeDefinitionRegistry.objectTypeExtensions(),
                "interfaceTypeExtensions", typeDefinitionRegistry.interfaceTypeExtensions(),
                "unionTypeExtensions", typeDefinitionRegistry.unionTypeExtensions(),
                "enumTypeExtensions", typeDefinitionRegistry.enumTypeExtensions(),
                "scalarTypeExtensions", typeDefinitionRegistry.scalarTypeExtensions(),
                "inputObjectTypeExtensions", typeDefinitionRegistry.inputObjectTypeExtensions(),
                "types", typeDefinitionRegistry.types(),
                "scalars", typeDefinitionRegistry.scalars(),
                // "directiveDefinitions", typeDefinitionRegistry.directiveDefinitions(),
                "schema", typeDefinitionRegistry.schemaDefinition()
        // "schemaExtensionDefinitions", typeDefinitionRegistry.schemaExtensionDefinitions(),
        // "schemaParseOrder", typeDefinitionRegistry.schemaParseOrder()
        );
        return Maps.of(targetProperty, graphql);
    }
}
