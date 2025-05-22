package com.example.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP server that exposes endpoints for the {@link TimeReportMCP}.
 */
public class TimeReportMCPServer {

    private final HttpServer server;

    /**
     * Creates a new server instance bound to the given port.
     *
     * @param port the port to bind to, or {@code 0} for any available port
     */
    public TimeReportMCPServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Mount manifest handler at /.well-known/mcp.json
        server.createContext("/.well-known/mcp.json", new ManifestHandler());
    }

    /** Starts the server. */
    public void start() {
        server.start();
    }

    /**
     * Stops the server after the given delay.
     *
     * @param delay the delay in seconds until the server is stopped
     */
    public void stop(int delay) {
        server.stop(delay);
    }

    /** Returns the port the server is bound to. */
    public int getPort() {
        return server.getAddress().getPort();
    }

    /**
     * Handler that serves a simple JSON document describing available endpoints
     * and the context format.
     */
    static class ManifestHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String json = "{\"version\":\"1.0\"," +
                    "\"description\":\"TimeReport MCP endpoints\"," +
                    "\"endpoints\":[\"/stats/{year}/{month}\"]}";

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
