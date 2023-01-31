package io.zenwave360.sdk.parsers;

import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

public class GraphQLParserTest {

    @Test
    public void testParseGraphQL() throws IOException {
        GraphQLParser parser = new GraphQLParser();
        parser.specFile = URI.create("classpath:io/zenwave360/sdk/resources/graphql/user-crud.graphql");
        var graphql = parser.parse();
        System.out.println(graphql);
        Assertions.assertNotNull(JSONPath.get(graphql, "$.graphql.types.Student"));
    }
}
