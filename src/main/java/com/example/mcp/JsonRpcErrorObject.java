package com.example.mcp;

public class JsonRpcErrorObject {
    private int code;
    private String message;
    private Object data; // Optional

    // Constructor for Gson deserialization (if ever needed) and manual creation
    public JsonRpcErrorObject(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public JsonRpcErrorObject(int code, String message) {
        this(code, message, null);
    }

    // Getters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
