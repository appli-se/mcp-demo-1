package com.example.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        server.createContext(BASE_PATH + "/.well-known/mcp.json",
                new LoggingHandler(new ManifestHandler()));
        server.createContext(BASE_PATH + "/time-report",
                new LoggingHandler(new TimeReportHandler()));
        server.createContext(BASE_PATH + "/search",
                new LoggingHandler(new SearchHandler()));
        server.createContext(BASE_PATH + "/fetch",
                new LoggingHandler(new FetchHandler()));
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
                    "\"version\": \"1.0\"," +
                    "\"description\": \"MCP tool manifest providing time reporting and search functionalities.\"," +
                    "\"tools\": [" +
                    "{" +
                    "\"name\": \"getTimeReportStats\"," +
                    "\"description\": \"Fetches time report statistics for a given year and month.\"," +
                    "\"base_path\": \"" + BASE_PATH + "/time-report\"," +
                    "\"parameters\": {" +
                    "\"type\": \"object\"," +
                    "\"properties\": {" +
                    "\"year\": {" +
                    "\"type\": \"integer\"," +
                    "\"description\": \"The year for the report.\"" +
                    "}," +
                    "\"month\": {" +
                    "\"type\": \"integer\"," +
                    "\"description\": \"The month for the report (1-12).\"" +
                    "}" +
                    "}," +
                    "\"required\": [\"year\", \"month\"]" +
                    "}" +
                    "}," +
                    "{" +
                    "\"name\": \"searchContent\"," +
                    "\"description\": \"Searches for content based on a query string.\"," +
                    "\"base_path\": \"" + BASE_PATH + "/search\"," +
                    "\"parameters\": {" +
                    "\"type\": \"object\"," +
                    "\"properties\": {" +
                    "\"query\": {" +
                    "\"type\": \"string\"," +
                    "\"description\": \"The search query.\"" +
                    "}" +
                    "}," +
                    "\"required\": [\"query\"]" +
                    "}" +
                    "}," +
                    "{" +
                    "\"name\": \"fetchContent\"," +
                    "\"description\": \"Fetches a specific content item by its ID.\"," +
                    "\"base_path\": \"" + BASE_PATH + "/fetch\"," +
                    "\"parameters\": {" +
                    "\"type\": \"object\"," +
                    "\"properties\": {" +
                    "\"id\": {" +
                    "\"type\": \"string\"," +
                    "\"description\": \"The ID of the content to fetch.\"" +
                    "}" +
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

    /** Handler that exposes time report statistics as JSON. */
    class TimeReportHandler implements HttpHandler {
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
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

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
            Gson gson = new GsonBuilder().create();
            return gson.toJson(entries);
        }
    }

    /** Handler that exposes a very small search API as JSON. */
    class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> params = parseQuery(uri.getRawQuery());
            String query = params.getOrDefault("query", "");

            List<SearchResult> results = searchMcp.search(query);
            String json = toJson(results);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String query) {
            if (query == null || query.isEmpty()) {
                return Map.of();
            }
            return Stream.of(query.split("&"))
                    .map(s -> s.split("=", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        }

        private String toJson(List<SearchResult> results) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(Map.of("results", results));
        }
    }

    /** Handler that fetches a single search result by id. */
    class FetchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> params = parseQuery(uri.getRawQuery());
            String id = params.get("id");

            SearchResult result = searchMcp.fetch(id);
            if (result == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String json = toJson(result);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String query) {
            if (query == null || query.isEmpty()) {
                return Map.of();
            }
            return Stream.of(query.split("&"))
                    .map(s -> s.split("=", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        }

        private String toJson(SearchResult result) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(result);
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
