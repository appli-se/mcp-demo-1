package com.example.mcp;

public class JsonRpcResponse {
    private final String jsonrpc = "2.0"; // Always "2.0" for responses
    private Object result;    // Mutually exclusive with error
    private JsonRpcErrorObject error; // Mutually exclusive with result
    private Object id;        // Should match request id

    // Constructor for success response
    public JsonRpcResponse(Object result, Object id) {
        this.result = result;
        this.id = id;
        this.error = null; // Explicitly null for clarity
    }

    // Constructor for error response
    public JsonRpcResponse(JsonRpcErrorObject error, Object id) {
        this.error = error;
        this.id = id;
        this.result = null; // Explicitly null for clarity
    }

    // Getters
    public String getJsonrpc() { return jsonrpc; }
    public Object getResult() { return result; }
    public JsonRpcErrorObject getError() { return error; }
    public Object getId() { return id; }
}
