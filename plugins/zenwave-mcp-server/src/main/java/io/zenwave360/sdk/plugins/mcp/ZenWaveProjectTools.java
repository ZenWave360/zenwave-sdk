package io.zenwave360.sdk.plugins.mcp;

import io.zenwave360.sdk.plugins.ZdlToMarkdownPlugin;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ZenWaveProjectTools {

    @Tool(description = "Project Markdown description with Task List from ZenWave ZDL file contents")
    public String getProjectTaskList(
            @ToolParam(description = "Attach a ZenWave ZDL file and it will be used to generate the markdowntask list") String zdlContent
    ) throws IOException {
        return ZdlToMarkdownPlugin.generateTaskList(zdlContent);
    }

    @Tool(description = "Get markdown with plantuml codefences for an aggregate UML from ZenWave ZDL file contents")
    public String getAggregateUML(
            @ToolParam(description = "Attach a ZenWave ZDL file and it will be used to generate the UML diagram") String zdlContent,
            @ToolParam(description = "The name of the aggregate to generate the UML diagram for") String aggregateName
    ) throws IOException {
        return ZdlToMarkdownPlugin.generateAggregateUML(zdlContent, aggregateName);
    }
}
