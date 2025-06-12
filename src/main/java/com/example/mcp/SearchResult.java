package com.example.mcp;

/**
 * Simple search result data object used by the search MCP.
 */
public class SearchResult {
    private final String id;
    private final String title;
    private final String text;
    private final String url;

    public SearchResult(String id, String title, String text, String url) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }
}
