# VirtuAI MCP Server

This is a Model Context Protocol (MCP) server that allows AI assistants to interact with the VirtuAI API Virtualization Platform.

## Features

- **List Services**: View all registered virtualized services (HTTP/MQ).
- **Create Service**: Register a new virtual service.
- **Delete Service**: Remove a service and its rules.
- **List HTTP Rules**: View all mock rules for HTTP services.
- **Create HTTP Rule**: Add a new mock rule with custom responses, status codes, and path patterns.
- **Import OpenAPI**: Automatically generate a virtual service from an OpenAPI/Swagger specification.

## Setup

### Prerequisites

- Node.js 18 or higher.
- VirtuAI Platform running (default: `http://localhost:8080`).

### Installation

1. Navigate to the `mcp-server` directory.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Build the server:
   ```bash
   npm run build
   ```

### Configuration for Claude Desktop

Add the following to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "virtuai": {
      "command": "node",
      "args": [
        "c:/Users/pc/.gemini/antigravity/scratch/platform/mcp-server/build/index.js"
      ],
      "env": {
        "VIRTUAI_BASE_URL": "http://localhost:8080"
      }
    }
  }
}
```

## Usage

Once configured, your AI assistant will have access to tools like `list_services`, `create_http_rule`, etc. You can ask it to:
- "Show me all active virtual services."
- "Create a mock for a GET /users endpoint that returns a list of users."
- "Import this OpenAPI spec and create a mock service for it."
