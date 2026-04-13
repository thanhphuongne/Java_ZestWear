#!/usr/bin/env bash
set -euo pipefail

# script dir
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PROJECT_PATH=${1:-.}
MAVEN_ARGS=${2:-"-DskipTests package"}

echo "Building project '${PROJECT_PATH}' using Maven inside Docker..."

docker run --rm -v "${PROJECT_ROOT}:/workspace" -w "/workspace/${PROJECT_PATH}" maven:3.8.8-openjdk-17 mvn ${MAVEN_ARGS}

echo "Docker-based build finished."
