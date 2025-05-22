# TimeReport MCP Demo

This project provides a minimal demonstration of a **Model Context Protocol (MCP)** service implemented in Java. The server exposes a very small set of endpoints for querying time report statistics. It is intended purely for illustration and contains only fixture data.

## Building

The project is built using [Maven](https://maven.apache.org/). To compile the sources and run the tests, execute:

```bash
mvn test
```

To produce a jar you can run:

```bash
mvn package
```

## Running the Server

`TimeReportMCPServer` includes a `main` method which starts an HTTP server. After building, you can launch the server with:

```bash
mvn exec:java -Dexec.mainClass="com.example.mcp.TimeReportMCPServer"
```

Alternatively you can run the class directly from your IDE or the compiled classes directory. The server listens on port `8080` by default. You can specify a custom port as the first command line argument.

## Endpoints

This demonstration exposes two HTTP endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/.well-known/mcp.json` | `GET` | Returns a JSON manifest describing the MCP implementation. |
| `/time-report?year=YYYY&month=MM` | `GET` | Returns time report statistics for the given year and month in JSON format. |

### Manifest format

The manifest endpoint returns JSON similar to:

```json
{
  "version": "1.0",
  "description": "TimeReport MCP endpoints",
  "endpoints": ["/stats/{year}/{month}"]
}
```

### Time report response

Querying `/time-report` responds with an array of objects:

```json
[
  {
    "signature": "NH",
    "hours": 80
  }
]
```

Only a single fixture entry is provided, so requests for `year=2025` and `month=5` return the data above, while all other requests return an empty array.

## Notes

This repository is intentionally minimal and meant solely as an MCP example. The server implementation is not complete and the data model is fixed in memory.
