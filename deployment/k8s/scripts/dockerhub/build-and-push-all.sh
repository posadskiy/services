#!/bin/bash
set -e

# build-and-push-all.sh - Build and push all services to Docker Hub
# Usage: ./build-and-push-all.sh <version>

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

# Auth Service
echo "🚀 Building and pushing Auth Service to Docker Hub..."
docker buildx build --platform linux/amd64 -f auth-service/Dockerfile.prod -t $DOCKERHUB_USERNAME/auth-service:$VERSION -t $DOCKERHUB_USERNAME/auth-service:$TAG_DATE auth-service/ --push

# User Service
echo "👤 Building and pushing User Service to Docker Hub..."
docker buildx build --platform linux/amd64 -f user-service/Dockerfile.prod -t $DOCKERHUB_USERNAME/user-service:$VERSION -t $DOCKERHUB_USERNAME/user-service:$TAG_DATE user-service/ --push

# Email Service
echo "📧 Building and pushing Email Service to Docker Hub..."
docker buildx build --platform linux/amd64 -f email-service/Dockerfile.prod -t $DOCKERHUB_USERNAME/email-service:$VERSION -t $DOCKERHUB_USERNAME/email-service:$TAG_DATE email-service/ --push

# Email Template Service
echo "📝 Building and pushing Email Template Service to Docker Hub..."
docker buildx build --platform linux/amd64 -f email-template-service/Dockerfile.prod -t $DOCKERHUB_USERNAME/email-template-service:$VERSION -t $DOCKERHUB_USERNAME/email-template-service:$TAG_DATE email-template-service/ --push

echo "✅ All images built and pushed to Docker Hub successfully!" 