#!/bin/bash
set -e

# build-and-push-user.sh - Build and push User Service to GCR
# Usage: ./build-and-push-user.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
PROJECT_ID=$(gcloud config get-value project)
REGISTRY="gcr.io/$PROJECT_ID"
TAG_DATE=$(date +%Y%m%d%H%M%S)

echo "👤 Building and pushing User Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f user-service/Dockerfile.prod \
  -t $REGISTRY/user-service:$VERSION \
  -t $REGISTRY/user-service:$TAG_DATE \
  user-service/ --push

echo "✅ User Service image built and pushed to GCR successfully!" 