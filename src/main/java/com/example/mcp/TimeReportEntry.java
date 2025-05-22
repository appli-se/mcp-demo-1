package com.example.mcp;

public class TimeReportEntry {
    private final String signature;
    private final int hours;

    public TimeReportEntry(String signature, int hours) {
        this.signature = signature;
        this.hours = hours;
    }

    public String getSignature() {
        return signature;
    }

    public int getHours() {
        return hours;
    }
}
