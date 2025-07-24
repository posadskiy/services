#!/bin/bash
set -e

# build-and-push-email.sh - Build and push Email Service to GCR
# Usage: ./build-and-push-email.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
PROJECT_ID=$(gcloud config get-value project)
REGISTRY="gcr.io/$PROJECT_ID"
TAG_DATE=$(date +%Y%m%d%H%M%S)

echo "📧 Building and pushing Email Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f email-service/Dockerfile.prod \
  -t $REGISTRY/email-service:$VERSION \
  -t $REGISTRY/email-service:$TAG_DATE \
  email-service/ --push

echo "✅ Email Service image built and pushed to GCR successfully!" 