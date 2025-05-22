package com.example.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TimeReportMCPTest {

    @Test
    public void testFixture() {
        TimeReportMCP mcp = new TimeReportMCP();
        List<TimeReportEntry> entries = mcp.getTimeReportStats(2025, 5);
        assertEquals(1, entries.size());
        TimeReportEntry entry = entries.get(0);
        assertEquals("NH", entry.getSignature());
        assertEquals(80, entry.getHours());
    }
}
