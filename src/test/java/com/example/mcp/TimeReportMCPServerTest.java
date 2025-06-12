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

        assertEquals("1.0", manifestJson.get("version").getAsString());
        assertTrue(manifestJson.has("description"));
        assertTrue(manifestJson.has("tools"));

        JsonArray tools = manifestJson.getAsJsonArray("tools");
        assertFalse(tools.isEmpty(), "Tools array should not be empty");

        // Check the first tool (getTimeReportStats)
        JsonObject firstTool = tools.get(0).getAsJsonObject();
        assertEquals("getTimeReportStats", firstTool.get("name").getAsString());
        assertTrue(firstTool.has("description"));
        assertTrue(firstTool.has("base_path"));
        assertTrue(firstTool.has("parameters"));

        JsonObject params = firstTool.getAsJsonObject("parameters");
        assertEquals("object", params.get("type").getAsString());
        assertTrue(params.has("properties"));
        assertTrue(params.getAsJsonObject("properties").has("year"));
        assertTrue(params.getAsJsonObject("properties").has("month"));

        JsonArray requiredParams = params.getAsJsonArray("required");
        assertTrue(requiredParams.contains(gson.toJsonTree("year")));
        assertTrue(requiredParams.contains(gson.toJsonTree("month")));

        // Optionally, check other tools if necessary, e.g. searchContent
        boolean foundSearchContent = false;
        for (int i = 0; i < tools.size(); i++) {
            JsonObject tool = tools.get(i).getAsJsonObject();
            if ("searchContent".equals(tool.get("name").getAsString())) {
                foundSearchContent = true;
                assertTrue(tool.has("parameters"));
                JsonObject searchParams = tool.getAsJsonObject("parameters");
                assertTrue(searchParams.getAsJsonObject("properties").has("query"));
                break;
            }
        }
        assertTrue(foundSearchContent, "Tool 'searchContent' should be present");
    }
}
