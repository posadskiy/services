# Auth Service

Micronaut 5 authentication service: email/password accounts plus Google and Apple OIDC.

## What it does

- `POST /users` and `POST /signup` — register with email + password (BCrypt cost 10 by default)
- `POST /login` — password login, returns access + refresh tokens
- `POST /oauth/token` — `refresh_token`, `password`, or `client_credentials`. The body is `application/x-www-form-urlencoded`, as in OAuth 2.1. The same fields are also accepted as JSON.
- `POST /oauth/revoke` / `POST /logout` — revoke a refresh token family
- `GET /oauth2/authorize/{google|apple}?return_to=` — start OIDC. `return_to` must be an exact URL from `OAUTH_ALLOWED_FRONTEND_REDIRECT_URLS`
- `GET|POST /oauth2/callback/{provider}` — provider callback (Apple uses `form_post`)
- `POST /oauth2/finalize` — exchange the one-time login code (optional frontend PKCE)
- `GET /keys` and `GET /.well-known/openid-configuration`
- `GET /account/me` and `POST /account/deactivate`

`id` on a user and `user_id` on a token are strings, the same value as the access token `sub`. `expires_in` stays a number of seconds. The database id stays a number.

Access tokens last **15 minutes**. Refresh tokens are opaque, stored as SHA-256 hashes, rotated on use, and expire after 30 days. Reuse of a rotated refresh token revokes the whole family.

The `system` password backdoor is gone. Machine clients use `grant_type=client_credentials` (`SYSTEM_CLIENT_ID` / `SYSTEM_CLIENT_SECRET`).

## Login latency

Password hashing is **not** the 10s delay. BCrypt cost 10 is about 50–150ms even on a 0.5 CPU pod. Cost 12 was rejected because it would be too slow on that hardware.

The previous ~10s login was from blocking work on the Netty event loop and/or a cold database connection (pool `minimum-idle: 0`). This build:

- runs the server with `thread-selection: AUTO` (virtual threads on JDK 25)
- marks auth controllers `@ExecuteOn(BLOCKING)`
- warms BCrypt at startup

Keep cost at 10. Do not raise it unless you give the pod more CPU.

## OAuth (Google + Apple)

This service implements OIDC itself. It does **not** use Micronaut Security OAuth2 (that would expose a second set of `/oauth/login` routes). Email/password signup and Google/Apple social login both issue the same platform tokens.

OAuth `state` and one-time login codes live in **Postgres** so multiple replicas work without Redis. Redis can replace those tables later.

Social accounts are auto-linked to an existing email only when the provider asserts `email_verified=true`. Provider access/refresh tokens are not stored.

## Run

Requires **JDK 25** and PostgreSQL.

```bash
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
export DATASOURCE_USERNAME=auth_user
export DATASOURCE_PASSWORD=auth_password
export JWT_GENERATOR_SIGNATURE_SECRET='at-least-32-characters-secret-key!!'
export GOOGLE_OAUTH_CLIENT_ID=...
export GOOGLE_OAUTH_CLIENT_SECRET=...
export OAUTH_FRONTEND_REDIRECT_URL=http://localhost:3000/oauth/callback
mvn mn:run -pl auth-service-web -am -Dmicronaut.environments=dev
```

Health: `GET /health/liveness` and `GET /health/readiness`.

## GitHub Actions

Pull requests and `main` run `mvn install` and `spotless:check` in this directory with Maven 3.9.12.

A tag `auth-service/vX.Y.Z` (the same version as `pom.xml`) publishes the Maven modules, pushes the production image to Docker Hub, and creates a GitHub Release. Deploy that image with `./deployment/scripts/deploy.sh <version>`.

## Test / build

```bash
mvn test          # Testcontainers PostgreSQL (Docker required)
mvn spotless:check
mvn install       # needed for Micronaut AOT (nested Maven reads sibling modules from ~/.m2)
```

## Configuration notes

- `JWT_GENERATOR_SIGNATURE_SECRET` — HS256 signing. Every service that accepts these tokens verifies with this variable. On auth-service only, the same value is also exposed as `AUTH_JWT_HMAC_SECRET`, because that is the environment name Micronaut binds to `auth.jwt.hmac-secret`.
- `AUTH_JWT_PRIVATE_KEY_PEM` + `AUTH_JWT_PUBLIC_KEY_PEM` — switch to ES256 and publish the public key at `/keys`
- `AUTH_BCRYPT_COST` — default 10
- `SYSTEM_CLIENT_ENABLED=true` plus client id/secret for service-to-service tokens
- Apple: `APPLE_OAUTH_ENABLED`, team id, key id, and `.p8` PEM (`APPLE_OAUTH_PRIVATE_KEY_PEM`)

Email verification and password-reset mail are **not** sent from this service. When `email-service` / `email-template-service` are back, wire them here instead of embedding SMTP.
