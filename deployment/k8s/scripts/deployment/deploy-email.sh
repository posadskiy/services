#!/bin/bash
# deploy-email.sh - Deploy email-service only
# Usage: ./deploy-email.sh <version>

set -e  # Exit on any error

# Get the directory where this script is located and set K8S_DIR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Check if version parameter is provided
if [ $# -eq 0 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v0.1.0"
  exit 1
fi

VERSION=$1

# Configuration
PROJECT_ID=$(gcloud config get-value project)
CLUSTER_NAME="autopilot-cluster-1"
NAMESPACE="microservices"

echo "📧 Deploying email-service only..."
echo "📦 Project ID: $PROJECT_ID"
echo "🏗️  Cluster: $CLUSTER_NAME"
echo "🏷️  Version: $VERSION"
echo "📁 Namespace: $NAMESPACE"

# Check required environment variables
echo "🔍 Checking required environment variables..."
REQUIRED_VARS=("JWT_GENERATOR_SIGNATURE_SECRET" "GITHUB_TOKEN" "GITHUB_USERNAME" "DEV_EMAIL_ADDRESS" "DEV_EMAIL_TOKEN")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -ne 0 ]; then
    echo "❌ Error: Missing required environment variables:"
    for var in "${MISSING_VARS[@]}"; do
        echo "   - $var"
    done
    echo ""
    echo "💡 Please set these environment variables before running the script:"
    echo "   export JWT_GENERATOR_SIGNATURE_SECRET='your-jwt-secret'"
    echo "   export GITHUB_TOKEN='your-github-token'"
    echo "   export GITHUB_USERNAME='your-github-username'"
    echo "   export DEV_EMAIL_ADDRESS='your-email'"
    echo "   export DEV_EMAIL_TOKEN='your-email-token'"
    exit 1
fi

echo "✅ All required environment variables are set"

# Check if cluster exists and get credentials
echo "🔍 Checking cluster access..."
if ! gcloud container clusters describe $CLUSTER_NAME --zone=europe-central2 > /dev/null 2>&1; then
    echo "❌ Cluster $CLUSTER_NAME not found in europe-central2"
    echo "💡 Please check the cluster name and zone, or run:"
    echo "   gcloud container clusters list"
    exit 1
fi

# Get cluster credentials
echo "🔐 Getting cluster credentials..."
gcloud container clusters get-credentials $CLUSTER_NAME --zone=europe-central2

# Deploy namespace
echo "📁 Creating namespace..."
kubectl apply -f "$K8S_DIR/namespace.yaml"

# Deploy ConfigMap and Secrets
echo "⚙️  Deploying ConfigMap and Secrets..."
envsubst < "$K8S_DIR/configmap.yaml" | kubectl apply -f -
envsubst < "$K8S_DIR/secrets.yaml" | kubectl apply -f -

# Deploy email-service with version substitution
echo "📧 Deploying email-service..."
export IMAGE_VERSION=$VERSION
envsubst < "$K8S_DIR/../email-service/k8s/email-service.yaml" | kubectl apply -f -

# Wait for email-service to be ready
echo "⏳ Waiting for email-service to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/email-service -n $NAMESPACE

echo "✅ Email-service deployment completed successfully!"
echo ""
echo "📋 Status:"
kubectl get pods -n $NAMESPACE
echo ""
echo "🌐 Services:"
kubectl get services -n $NAMESPACE
echo ""
echo "💡 To access the email-service:"
echo "   kubectl get service email-service -n $NAMESPACE"
echo ""
echo "🔍 To view logs:"
echo "   kubectl logs -f deployment/email-service -n $NAMESPACE" 