# AGENTS.md

This repository holds sibling Micronaut services, a root Compose file, and shared cluster config. Each service is its own Maven project. Run Maven from that service's directory.

## Scope

Work in `auth-service/` and `user-service/`.

Ignore `email-service/` and `email-template-service/`. Do not read them for context, edit them, test them, build them, or start them. Leave their manifests, scripts, and the Compose `email` profile alone unless the user explicitly asks for those services.

## Layout

| Path | What it is |
| --- | --- |
| `auth-service/` | Authentication. Modules `auth-service-api`, `auth-service-core`, `auth-service-web`. Port **8100**. |
| `user-service/` | Users. Modules `user-service-api`, `user-service-core`, `user-service-web`. Port **8095**. |
| `docker-compose.dev.yml` | Local stack for the services above, plus Postgres on **5432**. |
| `deployment/` | Cluster prep only: namespace, ConfigMap, secrets, Traefik, scripts. It does not deploy a service. |
| `<service>/deployment/` | That service's Kubernetes manifest and `scripts/deploy.sh` / `scripts/build-and-push.sh`. |

Service commands and the auth HTTP API are in each service's `README.md`. Root `README.md` covers Compose, tags, and cluster prep.

## Toolchain

- **JDK 25.** Compilation fails on 21.
- **Maven 3.9.12** (`mvn` on `PATH`). This is the Maven in `maven:3.9.12-amazoncorretto-25-alpine`. There is no Maven wrapper.
- From a service directory: `mvn test`, `mvn spotless:check`, `mvn install`. Micronaut AOT's nested Maven resolves sibling modules from the local repo, so `install` (with `-am` when running one module) is the build that makes `mn:run` work.
- Run a service with `mvn mn:run -pl <service>-web -am -Dmicronaut.environments=dev`. `run` is not a Maven lifecycle phase.
- Auth and user resolve dependencies from Maven Central. A GitHub token is for publishing, not for compiling these two.
- Releases are tags `auth-service/v*` and `user-service/v*`. The tag version must match that service's `pom.xml`.

## auth-service

Postgres is required to run the app. Tests use Testcontainers PostgreSQL, so Docker must be running; they do not use the local Postgres service.

The `dev` profile fails to boot unless these are set. OAuth values can be dummies:

```
DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
DATASOURCE_USERNAME=auth_user
DATASOURCE_PASSWORD=auth_password
JWT_GENERATOR_SIGNATURE_SECRET=<at-least-32-characters>
OAUTH_FRONTEND_REDIRECT_URL=http://localhost:3000/oauth/callback
GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET
FACEBOOK_OAUTH_CLIENT_ID / FACEBOOK_OAUTH_CLIENT_SECRET
GITHUB_OAUTH_CLIENT_ID / GITHUB_OAUTH_CLIENT_SECRET
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

`OTEL_EXPORTER_OTLP_ENDPOINT` must be a full URL. A bare port makes startup fail with `OTLP endpoint must be a valid URL`. A collector does not have to be listening.

Health checks are `GET /health/liveness` and `GET /health/readiness`. On auth-service, `JWT_GENERATOR_SIGNATURE_SECRET` is also exposed as `AUTH_JWT_HMAC_SECRET` because that is the name Micronaut binds to `auth.jwt.hmac-secret`. Keep `AUTH_BCRYPT_COST` at 10.

## user-service

Same JDK, Maven, and `mn:run` shape as auth, with `-pl user-service-web`. It shares the Compose Postgres instance. Default API port is **8095**.
