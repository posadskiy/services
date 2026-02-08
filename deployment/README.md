# Shared Services Configuration

This repo holds **shared configuration** for the microservices platform: Docker Compose to run all services locally, and the **`deployment/`** folder to prepare the k3s cluster and build images. Individual services (auth, user, email, email-template) live in sibling directories and have their own READMEs.

---

## What’s in this repo

- **`docker-compose.dev.yml`** / **`docker-compose.prod.yml`** – run all services (and DB) from here
- **`deployment/`** – scripts and manifests to prepare the cluster (namespace, ConfigMap, Secrets, Traefik) and build/push images; services are deployed from each service’s `k8s/` folder

---

## Docker Compose (local run)

### Prerequisites

- Docker and Docker Compose  
- Optional networks: `user-web-network`, `observability-stack-network`  
  `docker network create user-web-network`  
  `docker network create observability-stack-network`

### Environment

Create a `.env` in this directory (or export in shell):

```bash
# Database
AUTH_DATABASE_NAME=auth_db
AUTH_DATABASE_USER=postgres
AUTH_DATABASE_PASSWORD=your_secure_password

# JWT
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here

# GitHub (Maven)
GITHUB_USERNAME=your_github_username
GITHUB_TOKEN=your_github_personal_access_token

# Email (SMTP)
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
EMAIL_PROTOCOL=smtp
EMAIL_AUTH=true
EMAIL_STARTTLS_ENABLE=true
EMAIL_DEBUG=true
```

### Commands

```bash
# Development
docker-compose -f docker-compose.dev.yml up -d
# Logs: docker-compose -f docker-compose.dev.yml logs -f

# Production
docker-compose -f docker-compose.prod.yml up
```

### Ports

| Service                | API  | Debug |
|------------------------|------|-------|
| Auth Service           | 8100 | 5005  |
| User Service           | 8095 | 5006  |
| Email Service          | 8090 | 5007  |
| Email Template Service | 8091 | 5008  |
| PostgreSQL             | 5432 | -     |

Jaeger UI: http://localhost:16686

---

## Deployment folder (`deployment/`)

The **`deployment/`** directory is used to **prepare** the k3s cluster and **build** images. It does **not** deploy individual services; each service is deployed from its own **`<service>/k8s/`** folder.

### What’s in `deployment/`

| Path | Purpose |
|------|--------|
| `configmap.yaml` | Shared ConfigMap (DB URLs, env) for namespace `microservices` |
| `secrets.yaml` | Shared Secrets (envsubst placeholders; set vars before apply) |
| `namespace.yaml` | Namespace `microservices` |
| `ingress/traefik-letsencrypt.yaml` | Cluster-level: Traefik ACME (Let’s Encrypt) in kube-system |
| `ingress/traefik-ingressroute.yaml` | Shared IngressRoute (e.g. api.posadskiy.com) |
| `ingress/traefik-middleware.yaml` | Shared middlewares (CORS, rate limit) |
| `scripts/common/get-version.sh` | Get version from a service’s pom.xml |
| `scripts/dockerhub/create-registry-secret.sh` | Create Docker Hub pull secret in a namespace |
| `scripts/dockerhub/build-and-push-all.sh` | Build and push all service images (calls each service’s build script) |
| `scripts/k3s/install-k3s.sh` | Install k3s on a server (args: &lt;server_ip&gt; &lt;ssh_user&gt;) |
| `scripts/k3s/setup-env.sh` | Check required env vars and cluster/registry access |
| `scripts/k3s/deploy-to-k3s.sh` | **Prepare cluster only**: namespace, secret, ConfigMap, Secrets, Traefik ingress (no service deployments) |

### What’s in each service (e.g. `auth-service/k8s/`)

- **`<service>.yaml`** – Kubernetes Deployment manifest  
- **`scripts/deploy.sh <version>`** – Deploy this service (cluster must already be prepared)  
- **`scripts/build-and-push.sh`** – Build and push this service’s image  

---

### How to use it (order of operations)

#### 1. Set environment variables (no defaults)

```bash
export DOCKERHUB_USERNAME=your-username
export DOCKERHUB_TOKEN=your-token
export K3S_SERVER_IP=your-server-ip
export K3S_SSH_USER=your-ssh-user
# Plus vars for deployment/secrets.yaml: AUTH_DATABASE_PASSWORD, JWT_GENERATOR_SIGNATURE_SECRET, GITHUB_*, etc.
```

#### 2. Install k3s (only if the server doesn’t have it)

```bash
cd deployment
./scripts/k3s/install-k3s.sh <server_ip> <ssh_user>
```

Configure `kubectl` (e.g. copy kubeconfig from the server).

#### 3. Prepare the cluster (once per cluster)

From **`shared-services-configuration/deployment`**:

```bash
./scripts/k3s/deploy-to-k3s.sh
```

Creates namespace, registry secret, ConfigMap, Secrets, Traefik. Does **not** deploy any service.

---

### Quick reference

| Goal | Where | Command |
|------|--------|--------|
| Prepare cluster | `deployment` | `./scripts/k3s/deploy-to-k3s.sh` |
| Install k3s | `deployment` | `./scripts/k3s/install-k3s.sh <ip> <user>` |
| Build/push all images | `deployment` | `./scripts/dockerhub/build-and-push-all.sh <version>` |
| Deploy one service | service folder | `./k8s/scripts/deploy.sh <version>` |
| Build/push one service | service folder | `./k8s/scripts/build-and-push.sh <version>` |

---
