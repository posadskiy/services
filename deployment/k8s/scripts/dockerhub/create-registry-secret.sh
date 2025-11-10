#!/bin/bash
set -e

# create-registry-secret.sh - Create Kubernetes secret for Docker Hub authentication
# Usage: ./create-registry-secret.sh [namespace]
# 
# Required environment variables:
#   DOCKERHUB_USERNAME - Docker Hub username (default: posadskiy)
#   DOCKERHUB_TOKEN - Docker Hub access token or password

NAMESPACE=${1:-"microservices"}
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:-"posadskiy"}

if [ -z "$DOCKERHUB_TOKEN" ]; then
  echo "Error: DOCKERHUB_TOKEN environment variable is required"
  echo "You can create a token at: https://hub.docker.com/settings/security"
  exit 1
fi

echo "Creating Docker Hub registry secret in namespace: $NAMESPACE"
echo "Using Docker Hub username: $DOCKERHUB_USERNAME"

# Create the secret using kubectl
kubectl create secret docker-registry dockerhub-registry-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username="$DOCKERHUB_USERNAME" \
  --docker-password="$DOCKERHUB_TOKEN" \
  --docker-email="${DOCKERHUB_EMAIL:-$DOCKERHUB_USERNAME@example.com}" \
  --namespace="$NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "✅ Docker Hub registry secret created successfully!"
echo "Secret name: dockerhub-registry-secret"
echo "Namespace: $NAMESPACE"

