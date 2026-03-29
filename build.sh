#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building QuestForge Plugin ==="
./gradlew :plugin:clean :plugin:build "$@"

JAR=$(find plugin/build/libs -name 'questforge-*.jar' | head -1)
if [ -n "$JAR" ]; then
    echo ""
    echo "Build successful: $JAR"
else
    echo "Build failed — no JAR produced." >&2
    exit 1
fi
