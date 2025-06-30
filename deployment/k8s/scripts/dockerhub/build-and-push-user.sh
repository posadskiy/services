#!/bin/bash
set -e

# build-and-push-user.sh - Build and push User Service to Docker Hub
# Usage: ./build-and-push-user.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:-"your-dockerhub-username"}
TAG_DATE=$(date +%Y%m%d%H%M%S)

if [ "$DOCKERHUB_USERNAME" = "your-dockerhub-username" ]; then
  echo "Please set your Docker Hub username in the DOCKERHUB_USERNAME environment variable."
  exit 1
fi

echo "👤 Building and pushing User Service to Docker Hub..."
docker buildx build --platform linux/amd64 -f user-service/Dockerfile.prod -t $DOCKERHUB_USERNAME/user-service:$VERSION -t $DOCKERHUB_USERNAME/user-service:$TAG_DATE user-service/ --push

echo "✅ User Service image built and pushed to Docker Hub successfully!" 