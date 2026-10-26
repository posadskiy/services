# Email Template Service

A Micronaut-based email template service that provides dynamic email template rendering with HTML support, template management, and comprehensive monitoring.

## 🚀 Features

- **Dynamic Template Rendering**: HTML and text email template processing
- **Template Management**: Create, update, and manage email templates
- **HTML Support**: Rich HTML email templates with styling
- **Template Variables**: Dynamic content insertion with variable substitution
- **Security**: JWT-based authentication for API access
- **PostgreSQL Database**: Persistent storage with Flyway migrations
- **Distributed Tracing**: Jaeger integration for observability
- **OpenAPI/Swagger**: Complete API documentation
- **Prometheus Metrics**: Built-in monitoring and metrics
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Architecture

### Core Components

- **Email Template Service**: Core template rendering functionality
- **Template Engine**: HTML and text template processing
- **Template Repository**: Database operations for template management
- **Email Form Mapper**: Data transformation for email forms
- **Database Migrations**: Flyway-managed schema evolution

### Template System

The service supports:
- **HTML Templates**: Rich formatted emails with CSS styling
- **Text Templates**: Plain text email alternatives
- **Variable Substitution**: Dynamic content insertion
- **Template Validation**: Input validation and sanitization

## 🛠️ Setup & Installation

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 18

### Environment Variables

Create a `.env` file in the email-template-service directory:

```bash
# Database Configuration
EMAIL_TEMPLATE_DATABASE_NAME=email_template_db
EMAIL_TEMPLATE_DATABASE_USER=email_template_user
EMAIL_TEMPLATE_DATABASE_PASSWORD=your_secure_password

# Email Service Integration
EMAIL_SERVICE_URL=http://email-service:8080

# JWT Configuration
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here
```

### Development Setup

From the repository root (email services use the `email` profile):

```bash
docker compose -f docker-compose.dev.yml --profile email up -d
```

- Service: http://localhost:8091
- Swagger UI: http://localhost:8091/swagger-ui/index.html
- Prometheus Metrics: http://localhost:8091/prometheus

## 🔧 Configuration

### Development Configuration (`application-dev.yml`)

- **Server Port**: 8300
- **CORS**: Enabled for localhost:3000
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics

### Production Configuration (`application-prod.yml`)

- **Jaeger Tracing**: 10% sampling
- **Enhanced Security**: Production-grade settings
- **Optimized Performance**: Production-optimized configurations

## 📡 API Endpoints

### Template Endpoints

- `POST /email/template/send` - Send templated email
- `GET /email/template/{id}` - Get template by ID
- `POST /email/template` - Create new template
- `PUT /email/template/{id}` - Update template
- `DELETE /email/template/{id}` - Delete template

### Documentation

- `GET /swagger-ui/index.html` - Swagger UI
- `GET /swagger/email-template-service-0.0.yml` - OpenAPI specification

## 🔍 Monitoring & Observability

### Jaeger Tracing

The service is configured to send distributed traces to Jaeger:

- **Development**: 100% sampling rate
- **Production**: 10% sampling rate
- **Jaeger UI**: http://localhost:16686

### Prometheus Metrics

Built-in metrics include:
- Template rendering metrics
- Email sending metrics
- HTTP request metrics
- JVM metrics

Access metrics at: `http://localhost:8091/prometheus`

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=EmailServiceTest

# Integration tests
mvn test -Dtest=EmailControllerIntegrationTest
```

### Test Coverage

The service includes comprehensive tests for:
- **API DTOs**: EmailFormDto serialization/deserialization
- **Core Services**: EmailService with template rendering
- **Web Controllers**: EmailController with HTTP endpoint testing
- **Integration Tests**: End-to-end template email sending flow
- **Authentication**: Security filter testing
- **Swagger UI**: Documentation accessibility

### Test Results

- **Total Tests**: 18 tests across all modules
- **API Module**: 3 tests (EmailFormDto serialization/deserialization)
- **Core Module**: 9 tests (services, mappers, models)
- **Web Module**: 6 tests (controllers, integration, authentication, Swagger UI)

## 🐳 Docker Deployment

### Development Build

```bash
docker build -t email-template-service:dev .
```

### Production Build

```bash
docker build -f Dockerfile.prod -t email-template-service:prod .
```

## 🔐 Security Features

- **JWT Authentication**: Secure API access with JWT tokens
- **Template Sanitization**: XSS protection for HTML templates
- **CORS Protection**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive request validation
- **Access Control**: Role-based template access

## 📊 Performance

- **Connection Pooling**: HikariCP for database connections
- **Template Caching**: Efficient template rendering
- **Async Processing**: Non-blocking I/O operations
- **Resource Management**: Efficient memory and CPU usage

## 🚀 Deployment

### Kubernetes

The service has its own **`deployment/`** folder: manifest `deployment/email-template-service.yaml` and scripts **`deployment/scripts/deploy.sh`**, **`deployment/scripts/build-and-push.sh`**. Shared cluster config (namespace, ConfigMap, Secrets, Traefik) lives in the repository **`deployment/`** directory.

```bash
# Deploy this service
export SHARED_K8S="$(pwd)/../deployment"
./deployment/scripts/deploy.sh <version>

# Build and push image
./deployment/scripts/build-and-push.sh <version>
```

### GitHub Actions

Pull requests and `main` run `mvn install` and `spotless:check` in this directory with Maven 3.9.12.

A tag `email-template-service/vX.Y.Z` (the same version as `pom.xml`) publishes the Maven modules, pushes the production image to Docker Hub, and creates a GitHub Release. Deploy that image with `./deployment/scripts/deploy.sh <version>`.

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
