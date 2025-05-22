package com.example.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
//<<<<<<< codex/complete-mcp-implementation-for-claude-desktop
import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple HTTP server exposing the TimeReportMCP over a network interface.
 */
public class TimeReportMCPServer {

    private final TimeReportMCP mcp;

    public TimeReportMCPServer(TimeReportMCP mcp) {
        this.mcp = mcp;
    }

    /**
     * Starts the HTTP server on the given port.
     */
    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/time-report", new TimeReportHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + port);
    }

    private class TimeReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            URI uri = exchange.getRequestURI();
            Map<String, String> params = parseQuery(uri.getRawQuery());
            int year = parseInt(params.get("year"), YearMonth.now().getYear());
            int month = parseInt(params.get("month"), YearMonth.now().getMonthValue());

            List<TimeReportEntry> entries = mcp.getTimeReportStats(year, month);
            String json = toJson(entries);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes();
//=======
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
//>>>>>>> main
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
//<<<<<<< codex/complete-mcp-implementation-for-claude-desktop

        private Map<String, String> parseQuery(String query) {
            if (query == null || query.isEmpty()) {
                return Map.of();
            }
            return Stream.of(query.split("&"))
                    .map(s -> s.split("=", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        }

        private int parseInt(String value, int defaultValue) {
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private String toJson(List<TimeReportEntry> entries) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < entries.size(); i++) {
                TimeReportEntry e = entries.get(i);
                sb.append('{')
                        .append("\"signature\":\"").append(e.getSignature()).append("\",")
                        .append("\"hours\":").append(e.getHours())
                        .append('}');
                if (i < entries.size() - 1) {
                    sb.append(',');
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port specified, using default 8080");
            }
        }
        TimeReportMCP mcp = new TimeReportMCP();
        TimeReportMCPServer server = new TimeReportMCPServer(mcp);
        server.start(port);
//=======
//>>>>>>> main
    }
}
