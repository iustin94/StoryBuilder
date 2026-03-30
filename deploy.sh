#!/usr/bin/env bash
set -e

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

./gradlew :plugin:build

cp plugin/build/libs/questforge-*.jar ~/HytaleServer/mods/

echo "Deployed to ~/HytaleServer/mods/"
