# Auth Service

A Micronaut-based authentication and authorization service with JWT token management, user registration, and distributed tracing using Jaeger.

## 🚀 Features

- **JWT-based Authentication**: Secure token-based authentication with access and refresh tokens
- **Social Sign-In**: OAuth 2.1/OIDC login with Google, Facebook (extensible for more providers)
- **User Registration & Management**: Complete user lifecycle management with email/password authentication
- **Password Security**: Secure password hashing and validation using BCrypt
- **Token Refresh**: Automatic token refresh mechanism with revocable refresh tokens
- **PostgreSQL Database**: Persistent storage with Flyway migrations
- **Distributed Tracing**: Jaeger integration for observability
- **OpenAPI/Swagger**: Complete API documentation
- **Prometheus Metrics**: Built-in monitoring and metrics
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Architecture

### Core Components

- **Authentication Provider**: Custom password-based authentication
- **JWT Token Management**: Access and refresh token generation/validation
- **User Repository**: Database operations for user management
- **Password Utilities**: Secure password encoding and matching
- **Database Migrations**: Flyway-managed schema evolution

### Database Schema

#### Users Table
```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

#### Refresh Tokens Table
```sql
CREATE TABLE refresh_token (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL,
    refresh_token TEXT         NOT NULL,
    revoked       BOOLEAN      NOT NULL,
    date_created  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## 🛠️ Setup & Installation

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+

### Environment Variables

Required environment variables (profile-specific defaults shown):

```bash
# Database Configuration (REQUIRED)
AUTH_DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>
  # dev: jdbc:postgresql://localhost:5432/auth_db (default)
  # docker: jdbc:postgresql://database:5432/auth_db
  # prod: Set via Kubernetes ConfigMap
AUTH_DATABASE_USER=auth_user
AUTH_DATABASE_PASSWORD=your_secure_password

# JWT Configuration (REQUIRED)
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here

# OAuth / Social Login (OPTIONAL - defaults provided for dev)
OAUTH_REDIRECT_BASE_URL=http://localhost:8100
OAUTH_TOKEN_ENCRYPTION_SECRET=32_character_min_secret
OAUTH_FRONTEND_REDIRECT_URL=http://localhost:3000/oauth/callback
GOOGLE_OAUTH_CLIENT_ID=google-client-id
GOOGLE_OAUTH_CLIENT_SECRET=google-client-secret
FACEBOOK_OAUTH_CLIENT_ID=facebook-app-id
FACEBOOK_OAUTH_CLIENT_SECRET=facebook-app-secret
APPLE_OAUTH_ENABLED=false
APPLE_OAUTH_CLIENT_ID=apple-service-id
APPLE_OAUTH_CLIENT_SECRET=apple-signed-client-secret
MICROSOFT_OAUTH_ENABLED=false
MICROSOFT_OAUTH_CLIENT_ID=microsoft-client-id
MICROSOFT_OAUTH_CLIENT_SECRET=microsoft-client-secret
GITHUB_OAUTH_ENABLED=false
GITHUB_OAUTH_CLIENT_ID=github-client-id
GITHUB_OAUTH_CLIENT_SECRET=github-client-secret
DISCORD_OAUTH_ENABLED=false
DISCORD_OAUTH_CLIENT_ID=discord-client-id
DISCORD_OAUTH_CLIENT_SECRET=discord-client-secret

# GitHub Packages (for Maven build only)
GITHUB_USERNAME=your_github_username
GITHUB_TOKEN=your_github_token
```

**Note**: The `AUTH_DATABASE_URL` variable name is used consistently across all profiles (not `DATASOURCE_URL`).

## OAuth provider setup (local redirect: `http://localhost:8100/oauth2/callback/{provider}`)

### Apple (web)
- Apple Developer → Identifiers → Service IDs → create a Service ID and enable **Sign in with Apple**.
- Add domain + redirect URL: `http://localhost:8100/oauth2/callback/apple`.
- Keys → create key with Sign in with Apple, download `.p8`, note **Key ID** and **Team ID**.
- Generate a signed client secret JWT (Team ID, Key ID, Service ID as client_id) and set `APPLE_OAUTH_CLIENT_SECRET`.
- Follow button styling per [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/sign-in-with-apple).

### Microsoft (personal + work/school)
- Azure Portal → App registrations → New registration (accounts in any identity provider).
- Redirect URI: `http://localhost:8100/oauth2/callback/microsoft`.
- Certificates & secrets → create client secret.
- API permissions → add `openid`, `profile`, `email`, `offline_access`.
- Use `MICROSOFT_OAUTH_CLIENT_ID` and secret; tenant `common`.

### GitHub
- GitHub → Settings → Developer settings → OAuth Apps → New OAuth App.
- Callback URL: `http://localhost:8100/oauth2/callback/github`.
- Copy Client ID and generate Client secret.
- Scopes: `read:user`, `user:email` for email access.

### Discord
- Discord Developer Portal → New Application.
- OAuth2 → General: set redirect `http://localhost:8100/oauth2/callback/discord`.
- OAuth2 → URL Generator: scopes `identify`, `email` (PKCE supported).
- Copy Client ID and Client Secret.

### Running the Application

The service supports three deployment modes:

#### 1. Development (Localhost) - `dev` profile

For local development with a database running on localhost:

```bash
# Option 1: Use defaults (localhost:5432)
./mvnw clean run -pl auth-service-web -Dmicronaut.environments=dev

# Option 2: Override database connection
export AUTH_DATABASE_URL=jdbc:postgresql://localhost:5432/auth_db
export AUTH_DATABASE_USER=auth_user
export AUTH_DATABASE_PASSWORD=your_password
export JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret
./mvnw clean run -pl auth-service-web -Dmicronaut.environments=dev

# Option 3: Run the JAR directly
java -jar -Dmicronaut.environments=dev auth-service-web/target/auth-service-web-*.jar
```

**Default database connection**: `jdbc:postgresql://localhost:5432/auth_db` (can be overridden)

#### 2. Docker Container - `docker` profile

For running in Docker containers (local or remote):

```bash
# Using docker-compose.dev.yml (includes database)
docker-compose -f docker-compose.dev.yml up

# Or build and run manually
docker build -t auth-service:latest .
docker run -e MICRONAUT_ENVIRONMENTS=docker \
  -e AUTH_DATABASE_URL=jdbc:postgresql://database:5432/auth_db \
  -e AUTH_DATABASE_USER=auth_user \
  -e AUTH_DATABASE_PASSWORD=your_password \
  -e JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret \
  auth-service:latest
```

**Database connection**: Uses Docker service name (e.g., `database:5432`)

#### 3. Production (k3s) - `prod` profile

For production deployment in Kubernetes/k3s, use this service's deployment scripts (they use shared config from `shared-services-configuration/deployment`):

```bash
# Deploy this service (applies shared namespace/ConfigMap/Secrets then auth-service manifest)
export SHARED_K8S="$(pwd)/../shared-services-configuration/deployment"
./deployment/scripts/deploy.sh <version>

# Build and push image
./deployment/scripts/build-and-push.sh <version>
```

Or prepare the cluster from shared config: `../shared-services-configuration/deployment/scripts/k3s/deploy-to-k3s.sh` (then deploy each service from its folder).

**Configuration**: Managed via Kubernetes ConfigMaps and Secrets in `shared-services-configuration/deployment`. The service uses the `prod` profile when deployed.

**Access the Service**:
- Service: http://localhost:8100 (dev/docker) or via k3s ingress (prod)
- Swagger UI: http://localhost:8100/swagger-ui/index.html
- Prometheus Metrics: http://localhost:8100/prometheus
- Health Check: http://localhost:8100/health

## 🔧 Configuration Profiles

### Development Profile (`application-dev.yml`)

- **Environment**: `dev`
- **Server Port**: 8100
- **Database**: localhost:5432 (default)
- **CORS**: Enabled for localhost:3000
- **JWT Access Token**: 360000ms (6 minutes)
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics
- **Use Case**: Local development on your machine

### Docker Profile (`application-docker.yml`)

- **Environment**: `docker`
- **Server Port**: 8100
- **Database**: Docker service name (e.g., `database:5432`)
- **CORS**: Configured for container networking
- **JWT Access Token**: 360000ms (6 minutes)
- **Jaeger Tracing**: 10% sampling
- **Prometheus**: Enabled
- **Use Case**: Docker containers (local or remote)

### Production Profile (`application-prod.yml`)

- **Environment**: `prod`
- **Server Port**: 8100
- **Database**: Kubernetes service endpoint
- **CORS**: Production settings
- **JWT Access Token**: Configurable via env var
- **Jaeger Tracing**: 1% sampling (configurable)
- **Prometheus**: Enabled
- **Health Checks**: Enabled
- **Use Case**: k3s/Kubernetes production deployment

## 📡 API Endpoints

### Authentication Endpoints

- `POST /login` - User authentication
- `POST /logout` - User logout
- `POST /refresh` - Token refresh

### OAuth 2.1 Endpoints

- `GET /oauth2/authorize/{provider}` - issues a signed authorization request (PKCE) for the selected provider. Supports Google (`provider=google`) and Facebook (`provider=facebook`) out of the box.
- `GET /oauth2/callback/{provider}` - exchanges the authorization code for provider tokens, links/creates the local account, and returns the platform JWT + refresh token pair.

### User Management

- `POST /users` - User registration
- `GET /users/{id}` - Get user by ID
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user

### Documentation

- `GET /swagger-ui/index.html` - Swagger UI
- `GET /swagger/auth-service-0.0.yml` - OpenAPI specification

## 🔍 Monitoring & Observability

### Jaeger Tracing

The service is configured to send distributed traces to Jaeger:

- **Development**: 100% sampling rate
- **Production**: 10% sampling rate
- **Jaeger UI**: http://localhost:16686

### Prometheus Metrics

Built-in metrics include:
- Database connection pool metrics
- HTTP request metrics
- JVM metrics
- Custom business metrics

Access metrics at: `http://localhost:8100/prometheus`

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=AuthServiceTest

# Integration tests
mvn test -Dtest=JwtAuthenticationTest
```

### Test Coverage

The service includes comprehensive tests for:
- **API DTOs**: UserDto serialization/deserialization
- **Core Services**: AuthenticationProviderUserPassword, PasswordEncoder, PasswordMatcher
- **Web Controllers**: HomeToSwaggerController, UserAlreadyExistsExceptionHandler
- **Integration Tests**: End-to-end authentication flow
- **Authentication**: Security filter testing
- **Swagger UI**: Documentation accessibility

### Test Results

- **Total Tests**: 15 tests across all modules
- **API Module**: 3 tests (UserDto serialization/deserialization)
- **Core Module**: 9 tests (services, utilities, exceptions)
- **Web Module**: 3 tests (controllers, integration, Swagger UI)

### Test Quality

- **Unit Tests**: Isolated testing with proper mocking
- **Integration Tests**: End-to-end HTTP endpoint testing
- **Exception Testing**: Comprehensive error scenario coverage
- **Serialization Testing**: DTO validation and transformation
- **Security Testing**: Authentication flow validation

### CI/CD Configuration

The service includes proper test configuration for CI environments:
- **Security Configuration**: Anonymous access to Swagger UI endpoints
- **Test Environment**: H2 in-memory database for testing
- **Mock Services**: Proper mocking for external dependencies

## 🐳 Docker Deployment

### Development Build

```bash
docker build -t auth-service:dev .
```

### Production Build

```bash
docker build -f Dockerfile.prod -t auth-service:prod .
```

### Docker Compose Networks

The service connects to:
- `user-web-network`: For inter-service communication
- `observability-stack-network`: For monitoring and tracing

## 🔐 Security Features

- **Password Hashing**: Secure password storage using BCrypt
- **JWT Tokens**: Stateless authentication with configurable expiration
- **Token Revocation**: Refresh token revocation capability
- **Provider Token Hardening**: AES-GCM encryption at rest for OAuth access/refresh tokens
- **CORS Protection**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive request validation
- **Access Control**: Role-based authentication

## 📊 Performance

- **Connection Pooling**: HikariCP for database connections
- **Caching**: Built-in caching mechanisms
- **Async Processing**: Non-blocking I/O operations
- **Resource Management**: Efficient memory and CPU usage

## 🚀 Deployment

### Kubernetes

The service has its own **`deployment/`** folder: manifest `deployment/auth-service.yaml` and scripts **`deployment/scripts/deploy.sh`**, **`deployment/scripts/build-and-push.sh`**. Shared cluster config (namespace, ConfigMap, Secrets, Traefik) lives in **`shared-services-configuration/deployment/`**.

```bash
# Deploy this service (requires SHARED_K8S or run from repo with default path)
export SHARED_K8S="$(pwd)/../shared-services-configuration/deployment"
./deployment/scripts/deploy.sh <version>

# Build and push image
./deployment/scripts/build-and-push.sh <version>
```

### GitHub Actions

Automated CI/CD pipeline includes:
- Automated testing
- Docker image building
- GitHub Packages publishing
- Release management

## 📝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the test cases for usage examples
