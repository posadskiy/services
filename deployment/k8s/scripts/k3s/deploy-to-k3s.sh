#!/bin/bash
set -e

# deploy-to-k3s.sh - Deploy microservices to k3s cluster
# Usage: ./deploy-to-k3s.sh [version]
# If version is not provided, extracts version from each service's pom.xml
#
# Required environment variables:
#   DOCKERHUB_USERNAME - Docker Hub username (default: posadskiy)
#   DOCKERHUB_TOKEN - Docker Hub access token
#   GITHUB_TOKEN - GitHub token for building images
#   GITHUB_USERNAME - GitHub username
#   JWT_GENERATOR_SIGNATURE_SECRET - JWT secret
#   AUTH_DATABASE_PASSWORD - Neon database password
#   DEV_EMAIL_ADDRESS - Email address for email service
#   DEV_EMAIL_TOKEN - Email service token
#   K3S_SERVER_IP - k3s server IP (default: 168.119.57.22)
#   K3S_SSH_USER - SSH user for k3s server (default: root)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
# PROJECT_ROOT should be the directory containing k8s/ (which is the project root)
PROJECT_ROOT="$(dirname "$K8S_DIR")"
COMMON_SCRIPTS_DIR="$K8S_DIR/scripts/common"

# Check if get-version script exists
if [ ! -f "$COMMON_SCRIPTS_DIR/get-version.sh" ]; then
  echo "Error: get-version.sh not found in $COMMON_SCRIPTS_DIR" >&2
  exit 1
fi

# If version is provided as argument, use it; otherwise extract from pom.xml
if [ $# -gt 0 ]; then
  VERSION=$1
  echo "Using provided version: $VERSION"
else
  echo "No version provided, extracting from pom.xml files..."
  VERSION=""
fi

# Default values
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:-"posadskiy"}
K3S_SERVER_IP=${K3S_SERVER_IP:-"168.119.57.22"}
K3S_SSH_USER=${K3S_SSH_USER:-"root"}
NAMESPACE="microservices"

echo "🚀 Deploying microservices to k3s cluster"
echo "📦 Version: $VERSION"
echo "🖥️  Server: $K3S_SSH_USER@$K3S_SERVER_IP"
echo "📁 Namespace: $NAMESPACE"
echo ""

# Validate environment
echo "🔍 Validating environment..."
"$SCRIPT_DIR/setup-env.sh" || {
  echo "❌ Environment validation failed"
  exit 1
}

# Check kubectl connectivity
if ! kubectl cluster-info &>/dev/null; then
  echo "❌ Error: Cannot connect to k3s cluster"
  echo "Please ensure KUBECONFIG is set correctly"
  exit 1
fi

echo "✅ Connected to k3s cluster"
kubectl get nodes

# Create namespace
echo ""
echo "📁 Creating namespace..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

# Create Docker Hub registry secret
echo ""
echo "🔐 Creating Docker Hub registry secret..."
"$K8S_DIR/scripts/dockerhub/create-registry-secret.sh" "$NAMESPACE"

# Deploy ConfigMap
echo ""
echo "⚙️  Deploying ConfigMap..."
kubectl apply -f "$K8S_DIR/configmap.yaml"

# Deploy Secrets (using envsubst for variable substitution)
echo ""
echo "🔒 Deploying Secrets..."
if command -v envsubst &> /dev/null; then
  envsubst < "$K8S_DIR/secrets.yaml" | kubectl apply -f -
else
  echo "⚠️  Warning: envsubst not found. Please manually update secrets.yaml with values"
  echo "   Then run: kubectl apply -f $K8S_DIR/secrets.yaml"
fi

# Function to deploy a service
deploy_service() {
  local SERVICE_NAME=$1
  local SERVICE_DIR="$PROJECT_ROOT/$SERVICE_NAME"
  local SERVICE_VERSION
  
  if [ -z "$VERSION" ]; then
    # Extract version from service's pom.xml
    SERVICE_VERSION=$("$COMMON_SCRIPTS_DIR/get-version.sh" "$SERVICE_DIR")
    echo "Extracted version for $SERVICE_NAME: $SERVICE_VERSION"
  else
    SERVICE_VERSION=$VERSION
  fi
  
  echo "  - Deploying $SERVICE_NAME (version: $SERVICE_VERSION)..."
  export IMAGE_VERSION=$SERVICE_VERSION
  envsubst < "$SERVICE_DIR/k8s/$SERVICE_NAME.yaml" | kubectl apply -f -
}

# Deploy all services
echo ""
echo "🚀 Deploying services..."

deploy_service "auth-service"
deploy_service "user-service"
deploy_service "email-service"
deploy_service "email-template-service"

# Deploy Traefik IngressRoute and Middleware
echo ""
echo "🌐 Deploying Traefik IngressRoute..."
kubectl apply -f "$K8S_DIR/ingress/traefik-middleware.yaml"
kubectl apply -f "$K8S_DIR/ingress/traefik-ingressroute.yaml"

# Wait for deployments
echo ""
echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/auth-service -n "$NAMESPACE" || true
kubectl wait --for=condition=available --timeout=300s deployment/user-service -n "$NAMESPACE" || true
kubectl wait --for=condition=available --timeout=300s deployment/email-service -n "$NAMESPACE" || true
kubectl wait --for=condition=available --timeout=300s deployment/email-template-service -n "$NAMESPACE" || true

# Show status
echo ""
echo "✅ Deployment completed!"
echo ""
echo "📋 Deployment Status:"
kubectl get pods -n "$NAMESPACE"
echo ""
echo "🌐 Services:"
kubectl get services -n "$NAMESPACE"
echo ""
echo "🔗 Ingress:"
kubectl get ingressroute -n "$NAMESPACE" || kubectl get ingress -n "$NAMESPACE"
echo ""
echo "💡 To view logs:"
echo "   kubectl logs -f deployment/auth-service -n $NAMESPACE"
echo "   kubectl logs -f deployment/user-service -n $NAMESPACE"
echo ""
echo "🔍 To check SSL certificate status:"
echo "   kubectl get certificates -n $NAMESPACE"
echo "   kubectl describe certificate -n $NAMESPACE"

