#!/bin/bash
set -e

# build-and-push-auth.sh - Build and push Auth Service to GCR
# Usage: ./build-and-push-auth.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
PROJECT_ID=$(gcloud config get-value project)
REGISTRY="gcr.io/$PROJECT_ID"
TAG_DATE=$(date +%Y%m%d%H%M%S)

echo "🚀 Building and pushing Auth Service to GCR..."
docker buildx build --platform linux/amd64 -f auth-service/Dockerfile.prod -t $REGISTRY/auth-service:$VERSION -t $REGISTRY/auth-service:$TAG_DATE auth-service/ --push

echo "✅ Auth Service image built and pushed to GCR successfully!" 