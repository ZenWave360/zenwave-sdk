package io.zenwave360.sdk.plugins.mcp;

import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ZenWaveProjectToolsTest {

    private final Parser parser = new ZDLParser();
    private final ZenWaveProjectTools zenWaveProjectTools = new ZenWaveProjectTools();

    @Test
    public void testGetAggregateUML() throws Exception {
        // Given
        String zdlContent = parser.loadSpecFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        String aggregateName = "CustomerOrder";

        // When
        String aggregateUML = zenWaveProjectTools.getAggregateUML(zdlContent, aggregateName);

        // Then
        assertNotNull(aggregateUML);
        assertTrue(aggregateUML.contains("```plantuml"));
        assertTrue(aggregateUML.contains(aggregateName));

        // Verify it matches the output from ZdlToMarkdownPlugin
        String expectedAggregateUML = ZdlToMarkdownPlugin.generateAggregateUML(zdlContent, aggregateName);
        assertEquals(expectedAggregateUML, aggregateUML);
    }
}
