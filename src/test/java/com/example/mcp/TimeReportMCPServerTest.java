package com.example.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.example.mcp.JsonRpcResponse;
// import com.example.mcp.TimeReportEntry; // Not strictly needed if asserting on Map structure
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TimeReportMCPServer}.
 */
public class TimeReportMCPServerTest {

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
    public void testManifestEndpoint() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/sse/.well-known/mcp.json";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();

        Gson gson = new Gson();
        JsonObject manifestJson = gson.fromJson(body, JsonObject.class);

        assertEquals("1.2", manifestJson.get("version").getAsString());
        assertTrue(manifestJson.has("description"));
        assertEquals("MCP service manifest defining JSON-RPC 2.0 methods.", manifestJson.get("description").getAsString());

        // Check service_endpoint
        assertTrue(manifestJson.has("service_endpoint"));
        JsonObject serviceEndpoint = manifestJson.getAsJsonObject("service_endpoint");
        assertEquals("/sse", serviceEndpoint.get("path").getAsString());
        assertEquals("json-rpc-2.0", serviceEndpoint.get("protocol").getAsString());
        assertEquals("POST", serviceEndpoint.get("http_method").getAsString());
        assertFalse(serviceEndpoint.has("request_schema")); // Should not be here

        assertTrue(manifestJson.has("methods"));
        JsonArray methods = manifestJson.getAsJsonArray("methods");
        assertFalse(methods.isEmpty(), "Methods array should not be empty");

        // Check getTimeReportStats method
        JsonObject getTimeReportStatsMethod = null;
        for (int i = 0; i < methods.size(); i++) {
            if ("getTimeReportStats".equals(methods.get(i).getAsJsonObject().get("name").getAsString())) {
                getTimeReportStatsMethod = methods.get(i).getAsJsonObject();
                break;
            }
        }
        assertNotNull(getTimeReportStatsMethod, "getTimeReportStats method not found");
        assertTrue(getTimeReportStatsMethod.has("description"));
        assertTrue(getTimeReportStatsMethod.has("params_schema"));
        JsonObject timeReportParamsSchema = getTimeReportStatsMethod.getAsJsonObject("params_schema");
        assertEquals("object", timeReportParamsSchema.get("type").getAsString());
        assertTrue(timeReportParamsSchema.getAsJsonObject("properties").has("year"));
        assertTrue(timeReportParamsSchema.getAsJsonObject("properties").has("month"));
        JsonArray timeReportRequiredParams = timeReportParamsSchema.getAsJsonArray("required");
        assertTrue(timeReportRequiredParams.contains(gson.toJsonTree("year")));
        assertTrue(timeReportRequiredParams.contains(gson.toJsonTree("month")));

        // Check searchContent method
        JsonObject searchContentMethod = null;
        for (int i = 0; i < methods.size(); i++) {
            if ("searchContent".equals(methods.get(i).getAsJsonObject().get("name").getAsString())) {
                searchContentMethod = methods.get(i).getAsJsonObject();
                break;
            }
        }
        assertNotNull(searchContentMethod, "searchContent method not found");
        assertTrue(searchContentMethod.has("description"));
        assertTrue(searchContentMethod.has("params_schema"));
        JsonObject searchParamsSchema = searchContentMethod.getAsJsonObject("params_schema");
        assertEquals("object", searchParamsSchema.get("type").getAsString());
        assertTrue(searchParamsSchema.getAsJsonObject("properties").has("query"));
        JsonArray searchRequiredParams = searchParamsSchema.getAsJsonArray("required");
        assertTrue(searchRequiredParams.contains(gson.toJsonTree("query")));

        // Check fetchContent method
        JsonObject fetchContentMethod = null;
        for (int i = 0; i < methods.size(); i++) {
            if ("fetchContent".equals(methods.get(i).getAsJsonObject().get("name").getAsString())) {
                fetchContentMethod = methods.get(i).getAsJsonObject();
                break;
            }
        }
        assertNotNull(fetchContentMethod, "fetchContent method not found");
        assertTrue(fetchContentMethod.has("description"));
        assertTrue(fetchContentMethod.has("params_schema"));
        JsonObject fetchParamsSchema = fetchContentMethod.getAsJsonObject("params_schema");
        assertEquals("object", fetchParamsSchema.get("type").getAsString());
        assertTrue(fetchParamsSchema.getAsJsonObject("properties").has("id"));
        JsonArray fetchRequiredParams = fetchParamsSchema.getAsJsonArray("required");
        assertTrue(fetchRequiredParams.contains(gson.toJsonTree("id")));
    }

    @Test
    public void testTimeReportStatsJsonRpc() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/sse";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        Map<String, Object> jsonRpcRequest = Map.of(
            "jsonrpc", "2.0",
            "method", "getTimeReportStats",
            "params", Map.of("year", 2025, "month", 5),
            "id", "test-timereport-1"
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
        assertEquals("test-timereport-1", jsonRpcResponse.getId());
        assertNull(jsonRpcResponse.getError());
        assertNotNull(jsonRpcResponse.getResult());

        // Result is List<TimeReportEntry>, which Gson deserializes into List<Map<String, Object>>
        assertTrue(jsonRpcResponse.getResult() instanceof List);
        List<?> resultList = (List<?>) jsonRpcResponse.getResult();
        assertFalse(resultList.isEmpty());
        assertTrue(resultList.get(0) instanceof Map);
        Map<String, Object> entryMap = (Map<String, Object>) resultList.get(0);
        assertEquals("NH", entryMap.get("signature"));
        // Gson deserializes numbers in Maps as Double by default
        assertEquals(80.0, entryMap.get("hours"));
    }
}
