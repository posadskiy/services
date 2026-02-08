#!/bin/bash
# get-version.sh - Extract version from pom.xml file
# Usage: ./get-version.sh [service_dir]
# If service_dir is not provided, extracts version from current directory's pom.xml

SERVICE_DIR=${1:-"."}

if [ ! -f "$SERVICE_DIR/pom.xml" ]; then
  echo "Error: pom.xml not found in $SERVICE_DIR" >&2
  exit 1
fi

# Extract version from pom.xml - look for the first <version> tag after <artifactId>
# This handles the case where version comes after artifactId in the POM structure
VERSION=$(grep -m 1 "<version>" "$SERVICE_DIR/pom.xml" | sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' | head -1)

if [ -z "$VERSION" ]; then
  echo "Error: Could not extract version from $SERVICE_DIR/pom.xml" >&2
  exit 1
fi

echo "$VERSION"

