#!/bin/bash
set -e
echo "=== create dir ==="
rm -rf "out" && mkdir "out"

echo "=== compiling ==="
find src -name "*.java" | xargs javac -d "out"
echo "=== compile finished  ==="
echo "=== Packaging ==="
jar cfm "server.jar" "./MANIFEST.MF" -C "out" .
echo "=== Package finished ==="
echo "Run with: java -jar server.jar"
