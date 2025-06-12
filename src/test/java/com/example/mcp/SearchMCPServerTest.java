package com.example.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.example.mcp.JsonRpcResponse;
// import com.example.mcp.SearchResult; // Not strictly needed if asserting on Map structure
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Simple tests for the search endpoint. */
public class SearchMCPServerTest {
    private TimeReportMCPServer server;

    @BeforeEach
    public void setUp() throws IOException {
        server = new TimeReportMCPServer(0);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void testSearchEndpoint() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/sse"; // Target /sse endpoint
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST"); // Use POST
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        // Construct JSON-RPC request body
        Map<String, Object> jsonRpcRequest = Map.of(
            "jsonrpc", "2.0",
            "method", "searchContent",
            "params", Map.of("query", "time"),
            "id", "test-search-1"
        );
        String requestBody = new Gson().toJson(jsonRpcRequest);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, conn.getResponseCode()); // JSON-RPC uses HTTP 200 for valid responses

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String responseBody = sb.toString();

        JsonRpcResponse jsonRpcResponse = new Gson().fromJson(responseBody, JsonRpcResponse.class);
        assertEquals("2.0", jsonRpcResponse.getJsonrpc());
        assertEquals("test-search-1", jsonRpcResponse.getId());
        assertNull(jsonRpcResponse.getError());
        assertNotNull(jsonRpcResponse.getResult());

        // Assuming result is Map<String, List<Map<String, Object>>> due to {"results": [...]}
        assertTrue(jsonRpcResponse.getResult() instanceof Map);
        Map<String, Object> resultData = (Map<String, Object>) jsonRpcResponse.getResult();
        assertTrue(resultData.containsKey("results"));
        assertTrue(resultData.get("results") instanceof List);
        List<?> resultsList = (List<?>) resultData.get("results");
        assertFalse(resultsList.isEmpty()); // Expecting fixtures to return something for "time"
    }

    @Test
    public void testFetchEndpoint() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/sse";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        Map<String, Object> jsonRpcRequest = Map.of(
            "jsonrpc", "2.0",
            "method", "fetchContent",
            "params", Map.of("id", "1"),
            "id", "test-fetch-1"
        );
        String requestBody = new Gson().toJson(jsonRpcRequest);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, conn.getResponseCode());

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String responseBody = reader.lines().collect(Collectors.joining());

        JsonRpcResponse jsonRpcResponse = new Gson().fromJson(responseBody, JsonRpcResponse.class);
        assertEquals("2.0", jsonRpcResponse.getJsonrpc());
        assertEquals("test-fetch-1", jsonRpcResponse.getId());
        assertNull(jsonRpcResponse.getError());
        assertNotNull(jsonRpcResponse.getResult());

        // Result should be a SearchResult object, which Gson deserializes into a Map by default here
        assertTrue(jsonRpcResponse.getResult() instanceof Map);
        Map<String, Object> searchResultMap = (Map<String, Object>) jsonRpcResponse.getResult();
        assertEquals("1", searchResultMap.get("id"));
        assertEquals("Time Report Overview", searchResultMap.get("title"));
    }

    @Test
    public void testFetchEndpointNotFound() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/sse";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        Map<String, Object> jsonRpcRequest = Map.of(
            "jsonrpc", "2.0",
            "method", "fetchContent",
            "params", Map.of("id", "non-existent-id"),
            "id", "test-fetch-not-found-1"
        );
        String requestBody = new Gson().toJson(jsonRpcRequest);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, conn.getResponseCode()); // Still HTTP 200

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String responseBody = reader.lines().collect(Collectors.joining());

        JsonRpcResponse jsonRpcResponse = new Gson().fromJson(responseBody, JsonRpcResponse.class);
        assertEquals("2.0", jsonRpcResponse.getJsonrpc());
        assertEquals("test-fetch-not-found-1", jsonRpcResponse.getId());
        assertNull(jsonRpcResponse.getError()); // No JSON-RPC error for "not found" data
        assertNull(jsonRpcResponse.getResult()); // The result itself is null
    }
}
