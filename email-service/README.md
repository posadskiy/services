# Email Service

A Micronaut service that sends transactional mail through Resend.

## 🚀 Features

- **Resend**: Transactional mail sent over the Resend HTTP API
- **Security**: JWT-based authentication for API access
- **PostgreSQL Database**: Persistent storage with Flyway migrations
- **Distributed Tracing**: Jaeger integration for observability
- **OpenAPI/Swagger**: Complete API documentation
- **Prometheus Metrics**: Built-in monitoring and metrics
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Architecture

### Core Components

- **Transactional sender**: Renders verification and password-reset mail and posts it to Resend

### Service Integration

Auth-service calls `POST /email/transactional` with a service JWT.

## 🛠️ Setup & Installation

### Prerequisites

- Java 25
- Maven 3.9+
- Docker & Docker Compose

### Environment Variables

Create a `.env` file in the email-service directory:

```bash
# JWT Configuration
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here
```

### Development Setup

From the repository root (email services use the `email` profile):

```bash
docker compose -f docker-compose.dev.yml up -d
```

- Service: http://localhost:8090
- Swagger UI: http://localhost:8090/swagger-ui/index.html
- Prometheus Metrics: http://localhost:8090/prometheus

## 🔧 Configuration

### Development Configuration (`application-dev.yml`)

- **Server Port**: 8200
- **CORS**: Enabled for localhost:3000
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics

### Production Configuration (`application-prod.yml`)

- **Jaeger Tracing**: 10% sampling
- **Enhanced Security**: Production-grade settings
- **Optimized Performance**: Production-optimized configurations

## 📡 API Endpoints

### Transactional mail

`POST /email/transactional` accepts a service JWT (`ROLE_SERVICE`). `verify-email` and `password-reset` are rendered
from HTML in this service, then sent through Resend. Auth-service calls this for verification and password reset.

```bash
RESEND_API_KEY=re_...
RESEND_ALLOWED_FROM=noreply@mail.grammarbar.com,noreply@mail.posadskiy.com
EMAIL_ALLOWED_LINK_ORIGINS=https://grammarbar.com,https://costy.posadskiy.com,https://repeaty.posadskiy.com
```

`EMAIL_ALLOWED_LINK_ORIGINS` limits `VERIFY_URL` and `RESET_URL` to those origins (scheme, host and port must match
exactly); anything else is answered with 400. It is required in production and optional in development.

`RESEND_API_KEY` and `RESEND_ALLOWED_FROM` are required. `RESEND_ALLOWED_FROM` is the comma-separated list of mailbox
addresses the caller may use. Auth-service sends the full From header, and this service rejects any address that is not
on the list. The HTML files are `verify-email` (`VERIFY_URL`) and `password-reset` (`RESET_URL`).

Mail leaves this service through Resend. There is no SMTP client.

### Documentation

- `GET /swagger-ui/index.html` - Swagger UI
- `GET /swagger/email-service-0.0.yml` - OpenAPI specification

## 🔍 Monitoring & Observability

### Jaeger Tracing

The service is configured to send distributed traces to Jaeger:

- **Development**: 100% sampling rate
- **Production**: 10% sampling rate
- **Jaeger UI**: http://localhost:16686

### Prometheus Metrics

Built-in metrics include:

- HTTP request metrics
- JVM metrics

Access metrics at: `http://localhost:8090/prometheus`

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=TransactionalEmailControllerTest
```

### Test Coverage

The service includes comprehensive tests for:

- **Core**: Resend sender
- **Web**: transactional endpoint
- **Authentication**: Security filter testing
- **Swagger UI**: Documentation accessibility

## 🐳 Docker Deployment

### Development Build

```bash
docker build -t email-service:dev .
```

### Production Build

```bash
docker build -f Dockerfile.prod -t email-service:prod .
```

## 🔐 Security Features

- **JWT Authentication**: Secure API access with JWT tokens
- **CORS Protection**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive request validation
- **TLS Support**: Encrypted email transmission

## 📊 Performance

- **Async Processing**: Non-blocking I/O operations
- **Resource Management**: Efficient memory and CPU usage

## 🚀 Deployment

### Kubernetes

The service has its own **`deployment/`** folder: manifest `deployment/email-service.yaml` and scripts **
`deployment/scripts/deploy.sh`**, **`deployment/scripts/build-and-push.sh`**. Shared cluster config (namespace,
ConfigMap, Secrets, Traefik) lives in the repository **`deployment/`** directory.

```bash
# Deploy this service
export SHARED_K8S="$(pwd)/../deployment"
./deployment/scripts/deploy.sh <version>

# Build and push image
./deployment/scripts/build-and-push.sh <version>
```

### GitHub Actions

Pull requests and `main` run `mvn install` and `spotless:check` in this directory with Maven 3.9.12.

A tag `email-service/vX.Y.Z` (the same version as `pom.xml`) publishes the Maven modules, pushes the production image to
Docker Hub, and creates a GitHub Release. Deploy that image with `./deployment/scripts/deploy.sh <version>`.

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
