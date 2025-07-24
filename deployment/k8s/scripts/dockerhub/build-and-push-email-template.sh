#!/bin/bash
set -e

# build-and-push-email-template.sh - Build and push Email Template Service to Docker Hub
# Usage: ./build-and-push-email-template.sh <version>

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

echo "📝 Building and pushing Email Template Service to Docker Hub..."
docker buildx build --platform linux/amd64 \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_USERNAME=$GITHUB_USERNAME \
  -f email-template-service/Dockerfile.prod \
  -t $DOCKERHUB_USERNAME/email-template-service:$VERSION \
  -t $DOCKERHUB_USERNAME/email-template-service:$TAG_DATE \
  email-template-service/ --push

echo "✅ Email Template Service image built and pushed to Docker Hub successfully!" 