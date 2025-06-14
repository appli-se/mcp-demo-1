package com.example.mcp;

/**
 * Simple search result data object used by the search MCP.
 */
import java.util.Map;

public class SearchResult {
    private final String id;
    private final String title;
    private final String text;
    private final String url;
    private final Map<String, String> metadata;

    public SearchResult(String id, String title, String text, String url) {
        this(id, title, text, url, null);
    }

    public SearchResult(String id, String title, String text, String url, Map<String, String> metadata) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.url = url;
        this.metadata = metadata;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
