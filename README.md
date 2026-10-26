# Shared services

Auth, user, email, and email-template services in one repository. Shared cluster config and the local Compose stack live in `deployment/`.

| Path | Contents |
|------|----------|
| `auth-service/` | Authentication service |
| `user-service/` | User service |
| `email-service/` | Email service |
| `email-template-service/` | Email template service |
| `deployment/` | Docker Compose, namespace, ConfigMap, secrets, Traefik, and cluster scripts |

Each service builds from its own directory (`./mvnw`, `Dockerfile`). Local stack:

```bash
cd deployment
docker compose -f docker-compose.dev.yml up -d
```

GitHub Actions live in `.github/workflows/` and run inside the service directory they name. Auth releases stay on tags `v*`. The other services release on tags `user-service/v*`, `email-service/v*`, and `email-template-service/v*`.
