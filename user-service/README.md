# User Service

Profile, per-product preferences and account lookups for the Posadskiy products. Port **8095**.

Authentication, registration, password and OAuth flows belong to [auth-service](../auth-service). This service
validates the access tokens that auth-service issues and never issues tokens itself.

## Data ownership

The service reads and writes the Postgres database that auth-service migrates (`users`, `user_social_identity`,
`product_preference`). Flyway is disabled here on purpose: auth-service owns the schema. Do not change columns
without a matching auth-service migration.

## Access rules

| Endpoint | Who can call it |
| --- | --- |
| `GET /v0/user/me`, `PATCH /v0/user/me` | The signed-in user |
| `GET/PUT /v0/user/me/preferences/{product}` | The signed-in user |
| `GET /v0/user/{id}` | The account owner or `ROLE_SERVICE` |
| `POST /v0/users/display-labels` | `ROLE_USER_LABELS` only (Costy profile machine client). Body `{ "ids": ["1","2"] }` (max 100). Returns `{ "id": "label" }` where label is `displayName` or email local-part. Missing/inactive ids omitted. |
| `GET /v0/user/{id}/identities`, `DELETE /v0/user/{id}/identities/{provider}` | The account owner or `ROLE_SERVICE` |
| `POST /v0/user/{id}/identities` | `ROLE_SERVICE` only. A link already owned by another account returns 409. |
| `DELETE /v0/user/{id}` | `ROLE_SERVICE` only. Anonymizes the account. Users erase their own account through auth-service. |
| `POST /signup` | `ROLE_SERVICE` only. Never marks an email as verified. |
| `GET /health`, `/health/liveness`, `/health/readiness` | Anonymous. Details are off in `prod`. |

Errors from the service layer return a JSON problem body with the matching status (404 for a missing user, 409 for
a conflicting identity, 403 for a deactivated account).

## Authentication

- `prod`: tokens are verified against auth-service's JWKS (`AUTH_JWKS_URL`, default `http://auth-service:8100/keys`)
  with issuer and audience checks. There is no shared HMAC secret in `prod`.
- `dev` and `docker`: auth-service falls back to an HMAC secret, so these profiles also accept
  `JWT_GENERATOR_SIGNATURE_SECRET`.

## Run

Needs JDK 25 and Maven 3.9.12. From the repository root:

```bash
docker compose -f docker-compose.dev.yml up -d
```

From this directory:

```bash
mvn test
mvn spotless:check
mvn install
mvn mn:run -pl user-service-web -am -Dmicronaut.environments=dev
```

Service: http://localhost:8095. Swagger UI is on in `dev` and `docker` only.

## Observability

- Traces go to the OTLP endpoint (`OTEL_EXPORTER_OTLP_ENDPOINT`). Production sampling defaults to 10%
  (`OTEL_TRACES_SAMPLER_ARG`).
- Prometheus metrics are at `/prometheus`. The ingress does not publish this path.

## Passwords

Passwords supplied to `POST /signup` are hashed with BCrypt at cost 10, the same as auth-service.
