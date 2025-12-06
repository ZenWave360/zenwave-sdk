# ZenWave MCP Server

Model Code Project (MCP) server for ZenWave SDK that provides tools for generating documentation and diagrams from ZDL files.

## Installation

### Prerequisites
- JBang installed on your system and available in your PATH
- An MCP Host Application like Claude Desktop, Visual Studio Code, or any other MCP client

### Configuration

Add the following configuration to your Claude Desktop settings:

```json
{
  "mcpServers": {
    "zenwave-mcp-server": {
        "type": "stdio",
        "command": "jbang",
        "args": [
            "--quiet",
            "io.zenwave360.sdk.plugins:zenwave-mcp-server:2.0.0-SNAPSHOT"
        ]
    }
  }
}
```

Or this command line to Visual Studio Code `Add MCP Server` wizard:

```shell
jbang --quiet io.zenwave360.sdk.plugins:zenwave-mcp-server:2.0.0-SNAPSHOT
```

## Available MCPTools

### Project Task List Generator

Generates a Markdown task list from a ZenWave ZDL file. The task list includes implementation tasks for services, aggregates, and entities.

Usage in Claude:
```
To generate a project task list, please attach your ZDL file and I'll help you create a detailed task list.
```

### Aggregate UML Diagram Generator

Generates PlantUML diagrams for specific aggregates from your ZDL model, including relationships and methods.

Usage in Claude:
```
To generate an aggregate UML diagram, please:
1. Attach your ZDL file
2. Specify the aggregate name you want to visualize
```

