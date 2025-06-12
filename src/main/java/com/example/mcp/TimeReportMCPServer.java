package com.example.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.example.mcp.JsonRpcRequest;
import com.example.mcp.JsonRpcResponse;
import com.example.mcp.JsonRpcErrorObject;
import com.example.mcp.JsonRpcErrorCodes;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Simple HTTP server that exposes endpoints for the {@link TimeReportMCP}.
 */
public class TimeReportMCPServer {

    private static final String BASE_PATH = "/sse";

    private final HttpServer server;
    // These fields are kept as they are passed to MainSsePostHandler.
    private final TimeReportMCP mcp;
    private final SearchMCP searchMcp;

    /**
     * Creates a new server bound to the given port using a default
     * {@link TimeReportMCP} instance.
     *
     * @param port the port to bind to, or {@code 0} for any free port
     */
    public TimeReportMCPServer(int port) throws IOException {
        this(new TimeReportMCP(), new SearchMCP(), port);
    }

    /**
     * Creates a new server bound to the given port using the provided MCP.
     *
     * @param mcp  the MCP backing this server
     * @param port the port to bind to
     */
    public TimeReportMCPServer(TimeReportMCP mcp, int port) throws IOException {
        this(mcp, new SearchMCP(), port);
    }

    /**
     * Creates a new server bound to the given port using the provided MCPs.
     */
    public TimeReportMCPServer(TimeReportMCP mcp, SearchMCP searchMcp, int port) throws IOException {
        this.mcp = mcp;
        this.searchMcp = searchMcp;

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Path for manifest, e.g., /sse/.well-known/mcp.json
        // (ManifestHandler was updated in a previous subtask to reflect the single /sse endpoint)
        server.createContext(BASE_PATH + "/.well-known/mcp.json",
                new LoggingHandler(new ManifestHandler()));

        // Main tool invocation endpoint, e.g., /sse (handles POST)
        server.createContext(BASE_PATH, // BASE_PATH is typically "/sse"
                new LoggingHandler(new MainSsePostHandler(mcp, searchMcp)));
    }

    /** Starts the server. */
    public void start() {
        server.start();
    }

    /** Stops the server after the given delay. */
    public void stop(int delay) {
        server.stop(delay);
    }

    /** Returns the port the server is bound to. */
    public int getPort() {
        return server.getAddress().getPort();
    }

    /** Simple handler that logs inbound requests before delegating. */
    static class LoggingHandler implements HttpHandler {
        private final HttpHandler delegate;

        LoggingHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            System.out.println("[DEBUG] Received " + method + " " + uri);
            delegate.handle(exchange);
        }
    }

    /** Handler returning a simple manifest describing available endpoints. */
    static class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String json = "{" +
                "\"version\": \"1.2\"," +
                "\"description\": \"MCP service manifest defining JSON-RPC 2.0 methods.\"," +
                "\"service_endpoint\": {" +
                    "\"path\": \"" + BASE_PATH + "\"," +
                    "\"protocol\": \"json-rpc-2.0\"," +
                    "\"http_method\": \"POST\"" +
                "}," +
                "\"methods\": [" +
                    "{" +
                        "\"name\": \"getTimeReportStats\"," +
                        "\"description\": \"Fetches time report statistics for a given year and month.\"," +
                        "\"params_schema\": {" + // Renamed from parameters_schema for consistency with example, though task said "parameters_schema"
                            "\"type\": \"object\"," +
                            "\"properties\": {" +
                                "\"year\": {\"type\": \"integer\", \"description\": \"The year for the report.\"}," +
                                "\"month\": {\"type\": \"integer\", \"description\": \"The month for the report (1-12).\"}" +
                            "}," +
                            "\"required\": [\"year\", \"month\"]" +
                        "}" +
                    "}," +
                    "{" +
                        "\"name\": \"searchContent\"," +
                        "\"description\": \"Searches for content based on a query string.\"," +
                        "\"params_schema\": {" +
                            "\"type\": \"object\"," +
                            "\"properties\": {" +
                                "\"query\": {\"type\": \"string\", \"description\": \"The search query.\"}" +
                            "}," +
                            "\"required\": [\"query\"]" +
                        "}" +
                    "}," +
                    "{" +
                        "\"name\": \"fetchContent\"," +
                        "\"description\": \"Fetches a specific content item by its ID.\"," +
                        "\"params_schema\": {" +
                            "\"type\": \"object\"," +
                            "\"properties\": {" +
                                "\"id\": {\"type\": \"string\", \"description\": \"The ID of the content to fetch.\"}" +
                            "}," +
                            "\"required\": [\"id\"]" +
                        "}" +
                    "}" +
                "]" +
            "}";

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class MainSsePostHandler implements HttpHandler {
        private final TimeReportMCP timeReportMcp;
        private final SearchMCP searchMcp;
        private final Gson gson;

        MainSsePostHandler(TimeReportMCP timeReportMcp, SearchMCP searchMcp) {
            this.timeReportMcp = timeReportMcp;
            this.searchMcp = searchMcp;
            this.gson = new GsonBuilder().create();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendHttpErrorResponse(exchange, 405, "Method Not Allowed. Only POST is supported for JSON-RPC.");
                return;
            }

            Object requestId = null;

            try {
                String requestBodyString;
                try (InputStream requestBodyStream = exchange.getRequestBody()) {
                    requestBodyString = new String(requestBodyStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("Error reading request body: " + e.getMessage());
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.PARSE_ERROR, "Failed to read request body.", e.getMessage());
                    sendJsonRpcErrorResponse(exchange, error, null);
                    return;
                }

                if (requestBodyString.isEmpty()) {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_REQUEST, "Request body is empty.", null);
                    sendJsonRpcErrorResponse(exchange, error, null);
                    return;
                }

                JsonRpcRequest jsonRpcRequest;
                try {
                    jsonRpcRequest = gson.fromJson(requestBodyString, JsonRpcRequest.class);
                    if (jsonRpcRequest != null) {
                        requestId = jsonRpcRequest.getId();
                    } else {
                        JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.PARSE_ERROR, "Parse error: Malformed JSON or input was 'null'.", requestBodyString);
                        sendJsonRpcErrorResponse(exchange, error, null);
                        return;
                    }
                } catch (JsonSyntaxException e) {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.PARSE_ERROR, "Parse error: " + e.getMessage(), requestBodyString);
                    sendJsonRpcErrorResponse(exchange, error, null);
                    return;
                }

                if (jsonRpcRequest.getJsonrpc() == null || !jsonRpcRequest.getJsonrpc().equals("2.0")) {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_REQUEST, "'jsonrpc' version must be '2.0'.", jsonRpcRequest.getJsonrpc());
                    sendJsonRpcErrorResponse(exchange, error, requestId);
                    return;
                }

                if (jsonRpcRequest.getMethod() == null || jsonRpcRequest.getMethod().trim().isEmpty()) {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_REQUEST, "'method' must be provided.", jsonRpcRequest.getMethod());
                    sendJsonRpcErrorResponse(exchange, error, requestId);
                    return;
                }

                String methodName = jsonRpcRequest.getMethod();
                Object paramsObject = jsonRpcRequest.getParams();
                Map<String, Object> paramsMap = null;

                if (paramsObject == null) {
                    paramsMap = Map.of();
                } else if (paramsObject instanceof Map) {
                    paramsMap = (Map<String, Object>) paramsObject;
                } else {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_PARAMS, "Invalid params: Parameters must be a JSON object or null.", paramsObject.getClass().getName());
                    sendJsonRpcErrorResponse(exchange, error, requestId);
                    return;
                }

                Object resultPayload;
                try {
                    switch (methodName) {
                        case "getTimeReportStats":
                            if (!paramsMap.containsKey("year") || !paramsMap.containsKey("month")) {
                                JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_PARAMS, "Missing 'year' or 'month' parameter for getTimeReportStats.", paramsMap);
                                sendJsonRpcErrorResponse(exchange, error, requestId);
                                return;
                            }
                            int year = ((Number) paramsMap.get("year")).intValue();
                            int month = ((Number) paramsMap.get("month")).intValue();
                            resultPayload = timeReportMcp.getTimeReportStats(year, month);
                            break;
                        case "searchContent":
                            if (!paramsMap.containsKey("query")) {
                                JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_PARAMS, "Missing 'query' parameter for searchContent.", paramsMap);
                                sendJsonRpcErrorResponse(exchange, error, requestId);
                                return;
                            }
                            String query = (String) paramsMap.get("query");
                            resultPayload = Map.of("results", searchMcp.search(query));
                            break;
                        case "fetchContent":
                            if (!paramsMap.containsKey("id")) {
                                JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_PARAMS, "Missing 'id' parameter for fetchContent.", paramsMap);
                                sendJsonRpcErrorResponse(exchange, error, requestId);
                                return;
                            }
                            String id = (String) paramsMap.get("id");
                            resultPayload = searchMcp.fetch(id);
                            break;
                        default:
                            JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found: " + methodName, methodName);
                            sendJsonRpcErrorResponse(exchange, error, requestId);
                            return;
                    }
                } catch (ClassCastException | NullPointerException e) {
                    JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INVALID_PARAMS, "Invalid parameter type or structure: " + e.getMessage(), paramsMap);
                    sendJsonRpcErrorResponse(exchange, error, requestId);
                    return;
                }

                if (requestId != null) {
                    JsonRpcResponse response = new JsonRpcResponse(resultPayload, requestId);
                    sendJsonRpcSuccessResponse(exchange, response);
                }

            } catch (Exception e) {
                System.err.println("Internal server error: " + e.getMessage());
                e.printStackTrace();
                JsonRpcErrorObject error = new JsonRpcErrorObject(JsonRpcErrorCodes.INTERNAL_ERROR, "Internal server error: " + e.getMessage(), e.getClass().getName());
                sendJsonRpcErrorResponse(exchange, error, requestId);
            }
        }

        private void sendJsonRpcSuccessResponse(HttpExchange exchange, JsonRpcResponse response) throws IOException {
            String jsonResponseString = gson.toJson(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = jsonResponseString.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void sendJsonRpcErrorResponse(HttpExchange exchange, JsonRpcErrorObject error, Object id) throws IOException {
            JsonRpcResponse response = new JsonRpcResponse(error, id);
            sendJsonRpcSuccessResponse(exchange, response); // JSON-RPC errors are still sent with HTTP 200
        }

        private void sendHttpErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
            Map<String, String> errorPayload = Map.of("error", errorMessage, "note", "This is an HTTP-level error, not a JSON-RPC structured error.");
            String jsonResponse = gson.toJson(errorPayload);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /** Simple main entry point starting the server on a port. */
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port specified, using default 8080");
            }
        }
        TimeReportMCPServer server = new TimeReportMCPServer(port);
        server.start();
    }
}
