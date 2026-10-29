# Email Service

A Micronaut-based email service that provides email sending capabilities with SMTP integration, template support, and comprehensive monitoring.

## 🚀 Features

- **SMTP Email Sending**: Secure email delivery via SMTP with authentication
- **Template Support**: HTML and text email templates
- **Multi-format Support**: Plain text and HTML email content types
- **User Service Integration**: Integration with user service for recipient validation
- **Security**: JWT-based authentication for API access
- **PostgreSQL Database**: Persistent storage with Flyway migrations
- **Distributed Tracing**: Jaeger integration for observability
- **OpenAPI/Swagger**: Complete API documentation
- **Prometheus Metrics**: Built-in monitoring and metrics
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Architecture

### Core Components

- **Email Service**: Core email sending functionality
- **SMTP Client**: Secure SMTP connection management
- **Template Engine**: Email template processing
- **User Client**: Integration with user service
- **Database Migrations**: Flyway-managed schema evolution

### Service Integration

The email service integrates with:
- **User Service**: For recipient validation and user information
- **Email Template Service**: For template-based email sending

## 🛠️ Setup & Installation

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 18
- SMTP Server (Gmail, SendGrid, etc.)

### Environment Variables

Create a `.env` file in the email-service directory:

```bash
# Database Configuration
EMAIL_DATABASE_NAME=email_db
EMAIL_DATABASE_USER=email_user
EMAIL_DATABASE_PASSWORD=your_secure_password

# SMTP Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
EMAIL_PROTOCOL=smtp
EMAIL_AUTH=true
EMAIL_STARTTLS_ENABLE=true

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
- **SMTP**: Development SMTP settings
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics

### Production Configuration (`application-prod.yml`)

- **Jaeger Tracing**: 10% sampling
- **Enhanced Security**: Production-grade settings
- **Optimized Performance**: Production-optimized configurations

## 📡 API Endpoints

### Transactional mail

`POST /email/transactional` accepts a service JWT (`ROLE_SERVICE`). `verify-email` and `password-reset` are rendered from HTML in this service, then sent through Resend. Auth-service calls this for verification and password reset.

```bash
RESEND_API_KEY=re_...
RESEND_ALLOWED_FROM=noreply@mail.grammarbar.com,noreply@mail.posadskiy.com
RESEND_WEBHOOK_SECRET=whsec_...
```

`RESEND_API_KEY` and `RESEND_ALLOWED_FROM` are required. `RESEND_ALLOWED_FROM` is the comma-separated list of mailbox addresses the caller may use. Auth-service sends the full From header, and this service rejects any address that is not on the list. The HTML files are `verify-email` (`VERIFY_URL`) and `password-reset` (`RESET_URL`). `POST /email/webhooks/resend` checks the Svix signature and records bounces and complaints.

The existing `POST /email/send/text` and `POST /email/send/html` routes stay on SMTP.

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
- Email sending metrics
- SMTP connection metrics
- HTTP request metrics
- JVM metrics

Access metrics at: `http://localhost:8090/prometheus`

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=EmailServiceTest

# Integration tests
mvn test -Dtest=EmailControllerTest
```

### Test Coverage

The service includes comprehensive tests for:
- **API DTOs**: EmailForm serialization/deserialization
- **Core Services**: EmailService with SMTP integration
- **Web Controllers**: EmailController with HTTP endpoint testing
- **Integration Tests**: End-to-end email sending flow
- **Authentication**: Security filter testing
- **Swagger UI**: Documentation accessibility

### Test Results

- **Total Tests**: 17 tests across all modules
- **API Module**: 3 tests (EmailForm serialization/deserialization)
- **Core Module**: 9 tests (services, models, exceptions)
- **Web Module**: 5 tests (controllers, integration, authentication, Swagger UI)

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
- **SMTP Authentication**: Secure email server authentication
- **CORS Protection**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive request validation
- **TLS Support**: Encrypted email transmission

## 📊 Performance

- **Connection Pooling**: HikariCP for database connections
- **SMTP Connection Pooling**: Efficient SMTP connection management
- **Async Processing**: Non-blocking I/O operations
- **Resource Management**: Efficient memory and CPU usage

## 🚀 Deployment

### Kubernetes

The service has its own **`deployment/`** folder: manifest `deployment/email-service.yaml` and scripts **`deployment/scripts/deploy.sh`**, **`deployment/scripts/build-and-push.sh`**. Shared cluster config (namespace, ConfigMap, Secrets, Traefik) lives in the repository **`deployment/`** directory.

```bash
# Deploy this service
export SHARED_K8S="$(pwd)/../deployment"
./deployment/scripts/deploy.sh <version>

# Build and push image
./deployment/scripts/build-and-push.sh <version>
```

### GitHub Actions

Pull requests and `main` run `mvn install` and `spotless:check` in this directory with Maven 3.9.12.

A tag `email-service/vX.Y.Z` (the same version as `pom.xml`) publishes the Maven modules, pushes the production image to Docker Hub, and creates a GitHub Release. Deploy that image with `./deployment/scripts/deploy.sh <version>`.

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
