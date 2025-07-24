#!/bin/bash
set -e

# build-and-push-all.sh - Build and push all services to GCR
# Usage: ./build-and-push-all.sh <version>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1
PROJECT_ID=$(gcloud config get-value project)
REGISTRY="gcr.io/$PROJECT_ID"
TAG_DATE=$(date +%Y%m%d%H%M%S)

# Auth Service
echo "🚀 Building and pushing Auth Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f auth-service/Dockerfile.prod \
  -t $REGISTRY/auth-service:$VERSION \
  -t $REGISTRY/auth-service:$TAG_DATE \
  auth-service/ --push

# User Service
echo "👤 Building and pushing User Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f user-service/Dockerfile.prod \
  -t $REGISTRY/user-service:$VERSION \
  -t $REGISTRY/user-service:$TAG_DATE \
  user-service/ --push

# Email Service
echo "📧 Building and pushing Email Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f email-service/Dockerfile.prod \
  -t $REGISTRY/email-service:$VERSION \
  -t $REGISTRY/email-service:$TAG_DATE \
  email-service/ --push

# Email Template Service
echo "📝 Building and pushing Email Template Service to GCR..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f email-template-service/Dockerfile.prod \
  -t $REGISTRY/email-template-service:$VERSION \
  -t $REGISTRY/email-template-service:$TAG_DATE \
  email-template-service/ --push

echo "✅ All images built and pushed to GCR successfully!" 