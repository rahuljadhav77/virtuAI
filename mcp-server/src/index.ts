import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  ListToolsRequestSchema,
  CallToolRequestSchema,
  ErrorCode,
  McpError,
} from "@modelcontextprotocol/sdk/types.js";
import axios from "axios";

const VIRTUAI_BASE_URL = process.env.VIRTUAI_BASE_URL || "http://localhost:8080";

class VirtuaiMcpServer {
  private server: Server;
  private axiosInstance;

  constructor() {
    this.server = new Server(
      {
        name: "virtuai-mcp-server",
        version: "1.2.0",
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.axiosInstance = axios.create({
      baseURL: VIRTUAI_BASE_URL,
      timeout: 10000,
    });

    this.setupToolHandlers();
    
    this.server.onerror = (error) => console.error("[MCP Error]", error);
    process.on('SIGINT', async () => {
      await this.server.close();
      process.exit(0);
    });
  }

  private setupToolHandlers() {
    this.server.setRequestHandler(ListToolsRequestSchema, async () => ({
      tools: [
        {
          name: "list_services",
          description: "List all virtualized services (HTTP and MQ)",
          inputSchema: { type: "object", properties: {} },
        },
        {
          name: "create_service",
          description: "Create a new virtualized service",
          inputSchema: {
            type: "object",
            properties: {
              name: { type: "string" },
              description: { type: "string" },
              type: { type: "string", enum: ["HTTP", "MQ"] },
              port: { type: "number" },
              basePath: { type: "string" },
            },
            required: ["name", "type"],
          },
        },
        {
          name: "delete_service",
          description: "Delete a service and all its rules",
          inputSchema: {
            type: "object",
            properties: { id: { type: "number" } },
            required: ["id"],
          },
        },
        {
          name: "list_http_rules",
          description: "List all HTTP mock rules",
          inputSchema: { type: "object", properties: {} },
        },
        {
          name: "create_http_rule",
          description: "Create a new HTTP mock rule",
          inputSchema: {
            type: "object",
            properties: {
              serviceId: { type: "number" },
              name: { type: "string" },
              pathPattern: { type: "string" },
              method: { type: "string" },
              responseBody: { type: "string" },
              statusCode: { type: "number" },
              priority: { type: "number" },
            },
            required: ["serviceId", "name", "pathPattern", "method"],
          },
        },
        {
          name: "get_traffic_logs",
          description: "Retrieve recorded traffic logs",
          inputSchema: {
            type: "object",
            properties: {
              serviceId: { type: "number", description: "Optional: filter by service ID" },
            },
          },
        },
        {
          name: "start_recording",
          description: "Start recording traffic from a target URL",
          inputSchema: {
            type: "object",
            properties: { targetUrl: { type: "string" } },
            required: ["targetUrl"],
          },
        },
        {
          name: "stop_recording",
          description: "Stop recording traffic",
          inputSchema: { type: "object", properties: {} },
        },
        {
          name: "get_health_report",
          description: "Get an AI-generated health report for a service",
          inputSchema: {
            type: "object",
            properties: { serviceId: { type: "number" } },
            required: ["serviceId"],
          },
        },
        {
          name: "analyze_mismatch",
          description: "Analyze why a request didn't match any rules",
          inputSchema: {
            type: "object",
            properties: {
              serviceId: { type: "number" },
              trafficLogId: { type: "number" },
            },
            required: ["serviceId", "trafficLogId"],
          },
        },
        {
          name: "auto_heal_rule",
          description: "Suggest and apply fixes to a mock rule based on a failed request",
          inputSchema: {
            type: "object",
            properties: {
              ruleId: { type: "number" },
              trafficLogId: { type: "number" },
            },
            required: ["ruleId", "trafficLogId"],
          },
        },
        {
          name: "import_openapi",
          description: "Import an OpenAPI specification",
          inputSchema: {
            type: "object",
            properties: {
              name: { type: "string" },
              spec: { type: "string" },
              type: { type: "string", default: "HTTP" },
            },
            required: ["name", "spec"],
          },
        },
      ],
    }));

    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        switch (request.params.name) {
          case "list_services": {
            const response = await this.axiosInstance.get("/api/admin/services");
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "create_service": {
            const response = await this.axiosInstance.post("/api/admin/services", request.params.arguments);
            return { content: [{ type: "text", text: `Service created: ${JSON.stringify(response.data)}` }] };
          }
          case "delete_service": {
            const { id } = request.params.arguments as { id: number };
            await this.axiosInstance.delete(`/api/admin/services/${id}`);
            return { content: [{ type: "text", text: `Service ${id} deleted.` }] };
          }
          case "list_http_rules": {
            const response = await this.axiosInstance.get("/api/admin/rules/http");
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "create_http_rule": {
            const response = await this.axiosInstance.post("/api/admin/rules/http", request.params.arguments);
            return { content: [{ type: "text", text: `HTTP Rule created: ${JSON.stringify(response.data)}` }] };
          }
          case "get_traffic_logs": {
            const params = request.params.arguments as { serviceId?: number };
            const response = await this.axiosInstance.get("/api/recorder/logs", { params });
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "start_recording": {
            const { targetUrl } = request.params.arguments as { targetUrl: string };
            const response = await this.axiosInstance.post(`/api/recorder/start?targetUrl=${encodeURIComponent(targetUrl)}`);
            return { content: [{ type: "text", text: response.data }] };
          }
          case "stop_recording": {
            const response = await this.axiosInstance.post("/api/recorder/stop");
            return { content: [{ type: "text", text: response.data }] };
          }
          case "get_health_report": {
            const { serviceId } = request.params.arguments as { serviceId: number };
            const response = await this.axiosInstance.get(`/api/ai/self-healing/health/${serviceId}`);
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "analyze_mismatch": {
            const { serviceId, trafficLogId } = request.params.arguments as { serviceId: number, trafficLogId: number };
            const response = await this.axiosInstance.get(`/api/ai/self-healing/analyze/${serviceId}/${trafficLogId}`);
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "auto_heal_rule": {
            const { ruleId, trafficLogId } = request.params.arguments as { ruleId: number, trafficLogId: number };
            const response = await this.axiosInstance.post(`/api/ai/self-healing/auto-heal?ruleId=${ruleId}&trafficLogId=${trafficLogId}`);
            return { content: [{ type: "text", text: JSON.stringify(response.data, null, 2) }] };
          }
          case "import_openapi": {
            const response = await this.axiosInstance.post("/api/admin/import/openapi", request.params.arguments);
            return { content: [{ type: "text", text: `Import successful: ${JSON.stringify(response.data)}` }] };
          }
          default:
            throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${request.params.name}`);
        }
      } catch (error: any) {
        return { isError: true, content: [{ type: "text", text: `Error: ${error.response?.data?.message || error.message}` }] };
      }
    });
  }

  async run() {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error("VirtuAI MCP server running on stdio");
  }
}

const server = new VirtuaiMcpServer();
server.run().catch(console.error);
