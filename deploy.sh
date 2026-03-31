#!/usr/bin/env bash
set -e

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

./gradlew :plugin:build :prefab-browser:build

cp plugin/build/libs/questforge-*.jar ~/HytaleServer/mods/
cp prefab-browser/build/libs/prefab-browser-*.jar ~/HytaleServer/mods/

echo "Deployed to ~/HytaleServer/mods/"
