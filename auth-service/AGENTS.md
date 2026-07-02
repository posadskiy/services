# AGENTS.md

## Cursor Cloud specific instructions

This repository is a Maven multi-module **Micronaut** auth service (`auth-service-api`, `auth-service-core`, `auth-service-web`). The web module is the runnable application (port **8100**). Standard commands live in `README.md`; only the non-obvious cloud caveats are captured here.

### Toolchain
- The build requires **JDK 25** (`maven.compiler.source/target=25` in the POMs), even though `README.md` and CI say "Java 21". JDK 25 is installed and is the default `java` (`/usr/lib/jvm/java-25-openjdk-amd64`, via `update-alternatives`). Do not downgrade to 21 — compilation fails.
- Use the Maven wrapper `./mvnw` (Maven 3.9.x). No `settings.xml`/GitHub token is needed: all dependencies resolve from Maven Central. The `github` repository in `pom.xml` is only for publishing.

### PostgreSQL (required to run the app, not for tests)
- Postgres 16 is installed but does **not** auto-start. Start it each session: `sudo service postgresql start`.
- A local dev database is provisioned and persists in the VM: database `auth_db`, role `auth_user` / password `auth_password`. Flyway applies migrations automatically on app startup.

### Required environment variables (dev run)
The `dev` profile has unresolved placeholders that make the app fail to boot unless these are set. OAuth values can be dummies (real OAuth flows are not exercised locally):
```
DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
DATASOURCE_USERNAME=auth_user
DATASOURCE_PASSWORD=auth_password
JWT_GENERATOR_SIGNATURE_SECRET=<>=32-char secret>
OAUTH_TOKEN_ENCRYPTION_SECRET=<32-char secret>
OAUTH_FRONTEND_REDIRECT_URL=http://localhost:3000/oauth/callback
GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET
FACEBOOK_OAUTH_CLIENT_ID / FACEBOOK_OAUTH_CLIENT_SECRET
GITHUB_OAUTH_CLIENT_ID / GITHUB_OAUTH_CLIENT_SECRET
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```
- **OTEL gotcha (non-obvious):** you must export a full-URL `OTEL_EXPORTER_OTLP_ENDPOINT`. Relying on the built-in placeholder default in `application.yml` (`${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}`) causes startup to crash with `OTLP endpoint must be a valid URL: 4317` (the OpenTelemetry SDK autoconfigure receives only the bare port). A collector need not actually be listening — export failures at runtime are non-fatal.
- The same OAuth-client env vars are also needed to run `./mvnw test`, otherwise web-module tests error with `Could not resolve placeholder ${GOOGLE_OAUTH_CLIENT_ID}`.

### Run / test / lint / build
- **Run (dev)** — the correct Micronaut goal is `mn:run` (README's `clean run` is wrong; `run` is not a lifecycle phase). Include `-am` so sibling modules build:
  `./mvnw mn:run -pl auth-service-web -am -Dmicronaut.environments=dev`
  Verify: `curl localhost:8100/health` → `{"status":"UP"}`; Swagger UI at `/swagger-ui/index.html`.
- **Test:** `./mvnw test` (web tests use an in-memory H2 DB; no Postgres needed).
- **Lint:** `./mvnw spotless:check` (Palantir Java format). Note: two files currently have pre-existing violations (`auth-service-core/.../oauth/OAuthProviderRegistry.java`, `OAuthStateStore.java`); `./mvnw spotless:apply` fixes them.
- **Build:** `./mvnw -DskipTests install` (AOT-optimized runnable jar at `auth-service-web/target/auth-service-web-*.jar`).

### Auth flow notes
- There is **no HTTP user-registration endpoint** (the README's `POST /users` does not exist). `/login` is Micronaut Security's built-in bearer login: `POST /login` with `{"username":"<email>","password":"<pw>"}`. Authentication looks users up by **email**; passwords are BCrypt (strength 8). To create a test user, insert into the `users` table with a BCrypt hash.
