#!/bin/sh
set -e

JUNIT_VERSION=1.9.3
JUNIT_JAR=junit-platform-console-standalone-$JUNIT_VERSION.jar

# Download JUnit console jar if not present
if [ ! -f "$JUNIT_JAR" ]; then
  echo "Downloading $JUNIT_JAR..."
  curl -L -o "$JUNIT_JAR" "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/$JUNIT_JAR"
fi

SRC_DIR=src/main/java
TEST_DIR=src/test/java
BIN_DIR=bin

mkdir -p "$BIN_DIR"

# Compile main sources
find "$SRC_DIR" -name '*.java' > main_sources.txt
javac --release 17 -d "$BIN_DIR" @main_sources.txt

# Compile test sources
find "$TEST_DIR" -name '*.java' > test_sources.txt
javac --release 17 -cp "$JUNIT_JAR:$BIN_DIR" -d "$BIN_DIR" @test_sources.txt

# Run tests using JUnit console launcher
java -jar "$JUNIT_JAR" -cp "$BIN_DIR" --scan-classpath
