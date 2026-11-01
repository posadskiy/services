#!/bin/bash
set -e

# deploy-to-k3s.sh - Initialize and prepare k3s cluster for deployments
# This script:
# - Creates namespace, Docker Hub registry secret, ConfigMap, Secrets
# - Deploys Traefik Let's Encrypt and shared IngressRoute/Middleware
#
# Required env: DOCKERHUB_USERNAME, DOCKERHUB_TOKEN, K3S_SERVER_IP, K3S_SSH_USER (see setup-env.sh)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

if [ -z "$DOCKERHUB_USERNAME" ] || [ -z "$DOCKERHUB_TOKEN" ]; then
  echo "Error: DOCKERHUB_USERNAME and DOCKERHUB_TOKEN environment variables are required" >&2
  exit 1
fi
if [ -z "$K3S_SERVER_IP" ] || [ -z "$K3S_SSH_USER" ]; then
  echo "Error: K3S_SERVER_IP and K3S_SSH_USER environment variables are required" >&2
  exit 1
fi
NAMESPACE="microservices"

echo "🚀 Preparing k3s cluster (namespace, config, ingress)"
echo "🖥️  Server: $K3S_SSH_USER@$K3S_SERVER_IP"
echo "📁 Namespace: $NAMESPACE"
echo ""

echo "🔍 Validating environment..."
"$SCRIPT_DIR/setup-env.sh" || {
  echo "❌ Environment validation failed"
  exit 1
}

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

# Deploy ConfigMap and Secrets (shared)
echo ""
echo "⚙️  Deploying ConfigMap..."
kubectl apply -f "$K8S_DIR/configmap.yaml"
kubectl apply -f "$K8S_DIR/network-policies.yaml"

echo ""
echo "🔒 Deploying Secrets..."
if command -v envsubst &> /dev/null; then
  # A PEM line that starts with --- is a YAML document separator once substituted.
  # Auth ignores whitespace inside the PEM, so store each key on one line.
  flatten_pem() {
    local name="$1"
    local value="${!name-}"
    value="${value//$'\r'/}"
    value="${value//$'\n'/}"
    printf -v "$name" '%s' "$value"
    export "$name"
  }
  flatten_pem AUTH_JWT_PRIVATE_KEY_PEM
  flatten_pem AUTH_JWT_PUBLIC_KEY_PEM
  envsubst < "$K8S_DIR/secrets.yaml" | kubectl apply -f -
else
  echo "⚠️  Warning: envsubst not found. Please manually update secrets.yaml with values"
  echo "   Then run: kubectl apply -f $K8S_DIR/secrets.yaml"
fi

# Deploy Traefik Let's Encrypt configuration (cluster-level)
echo ""
echo "🔐 Deploying Traefik Let's Encrypt configuration..."
kubectl apply -f "$K8S_DIR/ingress/traefik-letsencrypt.yaml"

echo "⏳ Waiting for Traefik to apply Let's Encrypt configuration..."
sleep 5

# Deploy Traefik IngressRoute and Middleware (shared ingress)
echo ""
echo "🌐 Deploying Traefik IngressRoute and Middleware..."
kubectl apply -f "$K8S_DIR/ingress/traefik-middleware.yaml"
kubectl apply -f "$K8S_DIR/ingress/traefik-ingressroute.yaml"

echo ""
echo "✅ Cluster prepared."
echo ""
echo "🔍 Traefik / Let's Encrypt:"
echo "   kubectl get helmchartconfig -n kube-system"
echo "   kubectl describe helmchartconfig traefik -n kube-system"
