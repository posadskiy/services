# Auth Service

Micronaut 5 authentication service: email/password accounts plus Google and Apple OIDC.

## What it does

- `POST /users` — register with email + password (BCrypt cost 10 by default)
- `POST /login` — password login, returns access + refresh tokens
- `POST /oauth/token` — `refresh_token`, `password`, or `client_credentials`
- `POST /oauth/revoke` / `POST /logout` — revoke a refresh token family
- `GET /oauth2/authorize/{google|apple}` — start OIDC (PKCE, nonce, browser-binding cookie)
- `GET|POST /oauth2/callback/{provider}` — provider callback (Apple uses `form_post`)
- `POST /oauth2/finalize` — exchange the one-time login code (optional frontend PKCE)
- `GET /keys` and `GET /.well-known/openid-configuration`
- Account: email verify, password reset, deactivate

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
./mvnw mn:run -pl auth-service-web -am -Dmicronaut.environments=dev
```

Health: `GET /health/liveness` and `GET /health/readiness`.

## Test / build

```bash
./mvnw test          # Testcontainers PostgreSQL (Docker required)
./mvnw spotless:check
./mvnw install       # needed for Micronaut AOT (nested Maven reads sibling modules from ~/.m2)
```

## Configuration notes

- `AUTH_JWT_HMAC_SECRET` / `JWT_GENERATOR_SIGNATURE_SECRET` — HS256 signing (existing secret still works)
- `AUTH_JWT_PRIVATE_KEY_PEM` + `AUTH_JWT_PUBLIC_KEY_PEM` — switch to ES256 and publish the public key at `/keys`
- `AUTH_BCRYPT_COST` — default 10
- `SYSTEM_CLIENT_ENABLED=true` plus client id/secret for service-to-service tokens
- Apple: `APPLE_OAUTH_ENABLED`, team id, key id, and `.p8` PEM (`APPLE_OAUTH_PRIVATE_KEY_PEM`)
