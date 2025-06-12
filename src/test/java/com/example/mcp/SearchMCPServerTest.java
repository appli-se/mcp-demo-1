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
        String url = "http://localhost:" + server.getPort() + "/search?query=time";
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
        assertTrue(body.contains("\"results\""));
    }

    @Test
    public void testFetchEndpoint() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/fetch?id=1";
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
        assertTrue(body.contains("\"id\""));
        assertTrue(body.contains("\"title\""));
        assertTrue(body.contains("\"text\""));
    }
}
