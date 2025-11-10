#!/bin/bash
set -e

# build-and-push-all.sh - Build and push all services to Docker Hub
# Usage: ./build-and-push-all.sh [version]
# If version is not provided, extracts version from each service's pom.xml

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"
COMMON_SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")/common"

# Check if get-version script exists
if [ ! -f "$COMMON_SCRIPTS_DIR/get-version.sh" ]; then
  echo "Error: get-version.sh not found in $COMMON_SCRIPTS_DIR" >&2
  exit 1
fi

DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:-"posadskiy"}
TAG_DATE=$(date +%Y%m%d%H%M%S)

# Check if Docker Hub credentials are set for private repository
if [ -z "$DOCKERHUB_TOKEN" ]; then
  echo "Warning: DOCKERHUB_TOKEN not set. You may need to login to Docker Hub manually:"
  echo "  docker login -u $DOCKERHUB_USERNAME"
fi

# If version is provided as argument, use it; otherwise extract from pom.xml
if [ $# -gt 0 ]; then
  VERSION=$1
  echo "Using provided version: $VERSION"
else
  echo "No version provided, extracting from pom.xml files..."
  VERSION=""
fi

# Function to build and push a service
build_and_push_service() {
  local SERVICE_NAME=$1
  local SERVICE_DIR="$PROJECT_ROOT/$SERVICE_NAME"
  local SERVICE_VERSION
  
  if [ -z "$VERSION" ]; then
    # Extract version from service's pom.xml
    SERVICE_VERSION=$("$COMMON_SCRIPTS_DIR/get-version.sh" "$SERVICE_DIR")
    echo "Extracted version for $SERVICE_NAME: $SERVICE_VERSION"
  else
    SERVICE_VERSION=$VERSION
  fi
  
  echo "🚀 Building and pushing $SERVICE_NAME to Docker Hub (version: $SERVICE_VERSION)..."
  # Build for ARM64 (server architecture is aarch64)
  docker buildx build --platform linux/arm64 \
    --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
    --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
    -f "$SERVICE_DIR/Dockerfile.prod" \
    -t $DOCKERHUB_USERNAME/$SERVICE_NAME:$SERVICE_VERSION \
    -t $DOCKERHUB_USERNAME/$SERVICE_NAME:$TAG_DATE \
    -t $DOCKERHUB_USERNAME/$SERVICE_NAME:latest \
    "$SERVICE_DIR/" --push
}

# Build and push all services
build_and_push_service "auth-service"
build_and_push_service "user-service"
build_and_push_service "email-service"
build_and_push_service "email-template-service"

echo "✅ All images built and pushed to Docker Hub successfully!" 