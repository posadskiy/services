# Shared services

Auth, user, email, and email-template services in one repository. Run them locally from the root Compose file. Prepare the cluster from [`deployment/`](deployment), then deploy each service from its own `deployment/` folder.

## Layout

| Path | Contents |
| --- | --- |
| [`auth-service/`](auth-service) | Authentication |
| [`user-service/`](user-service) | Users |
| [`email-service/`](email-service) | Email delivery |
| [`docker-compose.dev.yml`](docker-compose.dev.yml) | Local stack |
| [`deployment/`](deployment) | Namespace, ConfigMap, secrets, Traefik, cluster scripts |

## GitHub Actions

[CI](.github/workflows/ci.yml) installs Maven 3.9.12, the same version as the Docker build image, and runs `mvn install` for every service on pull requests and on `main`.

[Release](.github/workflows/release.yml) runs when a tag is pushed. The tag version must match that service's `pom.xml`. It publishes the Maven modules to GitHub Packages, pushes the `Dockerfile.prod` image to Docker Hub (`linux/arm64`, version and `latest`), and opens a GitHub Release. Roll that image out with `./deployment/scripts/deploy.sh <version>` from the service directory.

| Tag | Service |
| --- | --- |
| `auth-service/v*` | auth-service |
| `user-service/v*` | user-service |
| `email-service/v*` | email-service |

Secrets: `DEPLOYMENT_GITHUB_TOKEN` (read and publish GitHub Packages), `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.

## Local stack

Put a `.env` in the repository root.

```dotenv
AUTH_DATABASE_NAME=auth_db
AUTH_DATABASE_USER=postgres
AUTH_DATABASE_PASSWORD=
JWT_GENERATOR_SIGNATURE_SECRET=
```

`GITHUB_USERNAME` and `GITHUB_TOKEN` are build args for email-template only. OAuth client variables and `DEV_EMAIL_ADDRESS` / `DEV_EMAIL_TOKEN` are optional. Auth and email also need `AUTH_EMAIL_FROM_GRAMMARBAR`, `AUTH_EMAIL_FROM_POSADSKIY`, `RESEND_API_KEY`, and `RESEND_ALLOWED_FROM`. Email-template starts only with the `email` profile. The email image compiles `user-service-api` from this repository.

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml --profile email up -d
```

| Service | API | Debug |
| --- | --- | --- |
| Auth | 8100 | 5005 |
| User | 8095 | 5006 |
| Email | 8090 | 5007 |
| Email template | 8091 | 5008 |
| PostgreSQL | 5432 | |

## Cluster

[`deployment/`](deployment) creates the namespace, registry secret, ConfigMap, Secrets, and Traefik resources. It does not roll out a service.

```bash
cd deployment
./scripts/k3s/install-k3s.sh <server_ip> <ssh_user>
./scripts/k3s/deploy-to-k3s.sh
```

`deploy-to-k3s.sh` needs `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `K3S_SERVER_IP`, and `K3S_SSH_USER`, plus the placeholders in [`deployment/secrets.yaml`](deployment/secrets.yaml).

From a service directory:

```bash
./deployment/scripts/build-and-push.sh <version>
./deployment/scripts/deploy.sh <version>
```
