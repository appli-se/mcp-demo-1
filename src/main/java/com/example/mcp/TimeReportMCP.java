package com.example.mcp;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of a model context protocol (MCP) for time report statistics.
 */
public class TimeReportMCP {

    private final Map<YearMonth, List<TimeReportEntry>> data = new HashMap<>();

    public TimeReportMCP() {
        // load fixtures
        List<TimeReportEntry> entries = new ArrayList<>();
        entries.add(new TimeReportEntry("NH", 80));
        data.put(YearMonth.of(2025, 5), entries);
    }

    /**
     * Returns the time report statistics for the given year and month.
     *
     * @param year  the year
     * @param month the month (1-based, January is 1)
     * @return list of time report entries
     */
    public List<TimeReportEntry> getTimeReportStats(int year, int month) {
        return data.getOrDefault(YearMonth.of(year, month), new ArrayList<>());
    }
}
