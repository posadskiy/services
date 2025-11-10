# Microservices Platform

A comprehensive microservices platform built with Micronaut, featuring authentication, user management, email services, and distributed tracing.

## 🏗️ Architecture Overview

This platform consists of four core microservices:

- **Auth Service**: JWT-based authentication and user registration
- **User Service**: User management and profile operations
- **Email Service**: SMTP email sending capabilities
- **Email Template Service**: Dynamic email template rendering

## 🚀 Services

### [Auth Service](auth-service/README.md)
- JWT-based authentication with access and refresh tokens
- User registration and password management
- PostgreSQL with Flyway migrations
- Comprehensive security features

### [User Service](user-service/README.md)
- Complete user lifecycle management
- User profile CRUD operations
- Integration with authentication service
- Secure password handling with BCrypt

### [Email Service](email-service/README.md)
- SMTP email sending with authentication
- Template support for HTML and text emails
- Integration with user service for validation
- Multi-format email content support

### [Email Template Service](email-template-service/README.md)
- Dynamic email template rendering
- HTML and text template processing
- Template variable substitution
- Template management and validation

## 🛠️ Technology Stack

- **Framework**: Micronaut 4.5.0
- **Language**: Java 21
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Authentication**: JWT tokens with refresh mechanism
- **Monitoring**: Jaeger for distributed tracing, Prometheus for metrics
- **Documentation**: OpenAPI/Swagger UI
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, Micronaut Test

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+

### Development Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd shared
   ```

2. **Start all services**:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

3. **Access the services**:
   - Auth Service: http://localhost:8100
   - User Service: http://localhost:8095
   - Email Service: http://localhost:8090
   - Email Template Service: http://localhost:8091

4. **Access Swagger UI**:
   - Auth Service: http://localhost:8100/swagger-ui/index.html
   - User Service: http://localhost:8095/swagger-ui/index.html
   - Email Service: http://localhost:8090/swagger-ui/index.html
   - Email Template Service: http://localhost:8091/swagger-ui/index.html

### Production Setup

```bash
docker-compose -f docker-compose.prod.yml up
```

## 🧪 Testing

### Comprehensive Test Coverage

All services include extensive test coverage:

- **Unit Tests**: Isolated component testing with proper mocking
- **Integration Tests**: End-to-end HTTP endpoint testing
- **API Tests**: DTO serialization/deserialization validation
- **Security Tests**: Authentication and authorization flow testing
- **Swagger UI Tests**: Documentation accessibility validation

### Test Results Summary

| Service | Total Tests | API Tests | Core Tests | Web Tests |
|---------|-------------|-----------|------------|-----------|
| Auth Service | 15 | 3 | 9 | 3 |
| User Service | 17 | 3 | 33 | 17 |
| Email Service | 17 | 3 | 9 | 5 |
| Email Template Service | 18 | 3 | 9 | 6 |

### Running Tests

```bash
# All services
mvn test

# Specific service
cd auth-service && mvn test

# Specific test class
mvn test -Dtest=UserServiceTest
```

## 🔍 Monitoring & Observability

### Distributed Tracing

All services are configured with Jaeger for distributed tracing:
- **Development**: 100% sampling rate
- **Production**: 10% sampling rate
- **Jaeger UI**: http://localhost:16686

### Metrics

Prometheus metrics are available at:
- Auth Service: http://localhost:8100/prometheus
- User Service: http://localhost:8095/prometheus
- Email Service: http://localhost:8090/prometheus
- Email Template Service: http://localhost:8091/prometheus

## 🔐 Security Features

- **JWT Authentication**: Secure token-based authentication across all services
- **Password Security**: BCrypt hashing for secure password storage
- **CORS Protection**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive request validation
- **Access Control**: Role-based access control
- **TLS Support**: Encrypted communication

## 🐳 Docker Deployment

### Development Build

```bash
# Build all services
docker-compose -f docker-compose.dev.yml build

# Run all services
docker-compose -f docker-compose.dev.yml up
```

### Production Build

```bash
# Build all services for production
docker-compose -f docker-compose.prod.yml build

# Run all services in production
docker-compose -f docker-compose.prod.yml up
```

## 🚀 Kubernetes Deployment

The platform includes Kubernetes manifests in the `k8s/` directory:

```bash
# Deploy all services to Kubernetes
kubectl apply -f k8s/

# Deploy specific service
kubectl apply -f k8s/services/auth-service.yaml
```

## 📊 Performance Features

- **Connection Pooling**: HikariCP for database connections
- **Async Processing**: Non-blocking I/O operations
- **Caching**: Built-in caching mechanisms
- **Resource Management**: Efficient memory and CPU usage
- **Load Balancing**: Kubernetes-native load balancing

## 🔧 Configuration

### Environment Variables

Each service requires specific environment variables. See individual service README files for detailed configuration:

- [Auth Service Configuration](auth-service/README.md#environment-variables)
- [User Service Configuration](user-service/README.md#environment-variables)
- [Email Service Configuration](email-service/README.md#environment-variables)
- [Email Template Service Configuration](email-template-service/README.md#environment-variables)

### Database Configuration

All services use PostgreSQL with Flyway migrations:
- **Auth Service**: `auth_db`
- **User Service**: `user_db`
- **Email Service**: `email_db`
- **Email Template Service**: `email_template_db`

## 📝 API Documentation

Each service provides comprehensive API documentation via Swagger UI:

- **Auth Service**: http://localhost:8100/swagger-ui/index.html
- **User Service**: http://localhost:8095/swagger-ui/index.html
- **Email Service**: http://localhost:8090/swagger-ui/index.html
- **Email Template Service**: http://localhost:8091/swagger-ui/index.html

## 🚀 CI/CD Pipeline

The platform includes automated CI/CD pipelines with:

- **Automated Testing**: Comprehensive test suites for all services
- **Docker Image Building**: Automated container image creation
- **GitHub Packages**: Container registry integration
- **Release Management**: Automated versioning and releases

## 📝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Development Guidelines

- Follow the existing code style and patterns
- Add comprehensive tests for new features
- Update documentation for API changes
- Ensure security best practices are followed

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the individual service documentation
- Review the test cases for usage examples
- Consult the API documentation via Swagger UI

## 🔗 Related Documentation

- [Docker Setup Guide](README-Docker.md)
- [Auth Service Documentation](auth-service/README.md)
- [User Service Documentation](user-service/README.md)
- [Email Service Documentation](email-service/README.md)
- [Email Template Service Documentation](email-template-service/README.md) 
