package com.example.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very small MCP providing search results for demonstration purposes.
 */
public class SearchMCP {

    private final List<SearchResult> fixtures = new ArrayList<>();
    private final Map<String, SearchResult> fetchFixtures = new HashMap<>();

    public SearchMCP() {
        SearchResult r = new SearchResult("1", "Time Report Overview",
                "Overview of the TimeReport MCP demo.", null);
        fixtures.add(r);
        fetchFixtures.put(r.getId(), r);
    }

    /**
     * Returns search results for the given query. The implementation simply
     * returns the fixture list when the query is not blank and contains either
     * 'time' or 'report'. Otherwise an empty list is returned.
     */
    public List<SearchResult> search(String query) {
        if (query == null) {
            return new ArrayList<>();
        }
        String q = query.toLowerCase();
        if (q.contains("time") || q.contains("report")) {
            return new ArrayList<>(fixtures);
        }
        return new ArrayList<>();
    }

    /**
     * Fetches a single result by id, or {@code null} if not found.
     */
    public SearchResult fetch(String id) {
        if (id == null) {
            return null;
        }
        return fetchFixtures.get(id);
    }
}
