#!/bin/bash
set -e

# deploy-auth-service.sh - Deploy auth-service to k3s cluster
# Usage: ./deploy-auth-service.sh [version]
# If version is not provided, extracts version from auth-service/pom.xml

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
PROJECT_ROOT="$(dirname "$K8S_DIR")"
COMMON_SCRIPTS_DIR="$K8S_DIR/scripts/common"
SERVICE_NAME="auth-service"
NAMESPACE="microservices"

if [ ! -f "$COMMON_SCRIPTS_DIR/get-version.sh" ]; then
  echo "Error: get-version.sh not found in $COMMON_SCRIPTS_DIR" >&2
  exit 1
fi

if [ $# -gt 0 ]; then
  VERSION=$1
  echo "Using provided version: $VERSION"
else
  VERSION=$("$COMMON_SCRIPTS_DIR/get-version.sh" "$PROJECT_ROOT/$SERVICE_NAME")
  echo "Using version from pom.xml: $VERSION"
fi

echo "🚀 Deploying $SERVICE_NAME to k3s cluster"
echo "📦 Version: $VERSION"
echo "📁 Namespace: $NAMESPACE"
echo ""

echo "🔍 Validating environment..."
"$SCRIPT_DIR/setup-env.sh" || { echo "❌ Environment validation failed"; exit 1; }

if ! kubectl cluster-info &>/dev/null; then
  echo "❌ Error: Cannot connect to k3s cluster"; exit 1
fi
echo "✅ Connected to k3s cluster"

echo ""
echo "📁 Creating namespace..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "🔐 Creating Docker Hub registry secret..."
"$K8S_DIR/scripts/dockerhub/create-registry-secret.sh" "$NAMESPACE"

echo ""
echo "⚙️  Deploying ConfigMap..."
kubectl apply -f "$K8S_DIR/configmap.yaml"

echo ""
echo "🔒 Deploying Secrets..."
if command -v envsubst &>/dev/null; then
  envsubst < "$K8S_DIR/secrets.yaml" | kubectl apply -f -
else
  echo "⚠️  envsubst not found. Run: envsubst < $K8S_DIR/secrets.yaml | kubectl apply -f -"
  exit 1
fi

echo ""
echo "🚀 Deploying $SERVICE_NAME (version: $VERSION)..."
export IMAGE_VERSION=$VERSION
envsubst < "$PROJECT_ROOT/$SERVICE_NAME/k8s/$SERVICE_NAME.yaml" | kubectl apply -f -

echo ""
echo "⏳ Waiting for deployment..."
kubectl wait --for=condition=available --timeout=300s "deployment/$SERVICE_NAME" -n "$NAMESPACE" || true

echo ""
echo "✅ $SERVICE_NAME deployment completed!"
kubectl get pods -n "$NAMESPACE" -l app=$SERVICE_NAME
echo ""
echo "💡 View logs: kubectl logs -f deployment/$SERVICE_NAME -n $NAMESPACE"
