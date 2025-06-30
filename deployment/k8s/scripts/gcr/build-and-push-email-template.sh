#!/bin/bash
set -e

# build-and-push-email-template.sh - Build and push Email Template Service to GCR
# Usage: ./build-and-push-email-template.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
PROJECT_ID=$(gcloud config get-value project)
REGISTRY="gcr.io/$PROJECT_ID"
TAG_DATE=$(date +%Y%m%d%H%M%S)

echo "📝 Building and pushing Email Template Service to GCR..."
docker buildx build --platform linux/amd64 -f email-template-service/Dockerfile.prod -t $REGISTRY/email-template-service:$VERSION -t $REGISTRY/email-template-service:$TAG_DATE email-template-service/ --push

echo "✅ Email Template Service image built and pushed to GCR successfully!" 