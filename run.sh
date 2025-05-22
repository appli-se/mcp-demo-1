#!/bin/sh
set -e

# Compile Java sources
SRC_DIR=src/main/java
BIN_DIR=bin
mkdir -p "$BIN_DIR"
find "$SRC_DIR" -name '*.java' > main_sources.txt
javac --release 17 -d "$BIN_DIR" @main_sources.txt

# Start the MCP server on localhost:8080
java -cp "$BIN_DIR" com.example.mcp.TimeReportMCPServer 8080
