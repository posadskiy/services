#!/bin/bash
set -e

# build-and-push.sh - Build and push email-service to Docker Hub
# Usage: ./build-and-push.sh <version>

SERVICE_NAME="email-service"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_K8S="$(dirname "$(dirname "$SCRIPT_DIR")")"
SERVICE_ROOT="$(dirname "$SERVICE_K8S")"

if [ -z "$DOCKERHUB_USERNAME" ] || [ -z "$DOCKERHUB_TOKEN" ]; then
  echo "Error: DOCKERHUB_USERNAME and DOCKERHUB_TOKEN environment variables are required" >&2
  exit 1
fi
if [ $# -lt 1 ]; then
  echo "Usage: $0 <version>" >&2
  exit 1
fi
VERSION=$1
TAG_DATE=$(date +%Y%m%d%H%M%S)

echo "🚀 Building and pushing $SERVICE_NAME to Docker Hub (version: $VERSION)..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f "$SERVICE_ROOT/Dockerfile.prod" \
  -t $DOCKERHUB_USERNAME/$SERVICE_NAME:$VERSION \
  -t $DOCKERHUB_USERNAME/$SERVICE_NAME:$TAG_DATE \
  -t $DOCKERHUB_USERNAME/$SERVICE_NAME:latest \
  "$SERVICE_ROOT/" --push

echo "✅ $SERVICE_NAME image built and pushed to Docker Hub successfully!"
