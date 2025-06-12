package com.example.mcp;

// Using Object for params and id to be flexible as per JSON-RPC spec
// (params can be array or object, id can be string, number, or null)

public class JsonRpcRequest {
    private String jsonrpc;
    private String method;
    private Object params; // Can be Map<String, Object> for named params or List<Object> for positional
    private Object id;     // Can be String, Number, or null

    // Getters are needed for access; setters might be useful for construction or testing
    public String getJsonrpc() { return jsonrpc; }
    public String getMethod() { return method; }
    public Object getParams() { return params; }
    public Object getId() { return id; }

    // Gson can typically set private fields, but setters can be added if there are issues
    // or for manual object construction in tests.
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public void setMethod(String method) { this.method = method; }
    public void setParams(Object params) { this.params = params; }
    public void setId(Object id) { this.id = id; }
}
