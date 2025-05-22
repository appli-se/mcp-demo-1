package com.example.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
        String url = "http://localhost:" + server.getPort() + "/.well-known/mcp.json";
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
        assertTrue(body.contains("version"));
        assertTrue(body.contains("description"));
        assertTrue(body.contains("endpoints"));
    }
}
