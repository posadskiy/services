# Auth Service

A Micronaut-based authentication and authorization service with JWT token management, user registration, and distributed tracing using Jaeger.

## 🚀 Features

- **JWT-based Authentication**: Secure token-based authentication with access and refresh tokens
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

Create a `.env` file in the auth-service directory:

```bash
# Database Configuration
AUTH_DATABASE_NAME=auth_db
AUTH_DATABASE_USER=auth_user
AUTH_DATABASE_PASSWORD=your_secure_password

# JWT Configuration
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here

# GitHub Packages (for Maven dependencies)
GITHUB_USERNAME=your_github_username
GITHUB_TOKEN=your_github_token
```

### Development Setup

1. **Clone and Navigate**:
   ```bash
   cd auth-service
   ```

2. **Start with Docker Compose**:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

3. **Access the Service**:
   - Service: http://localhost:8100
   - Swagger UI: http://localhost:8100/swagger-ui/index.html
   - Prometheus Metrics: http://localhost:8100/prometheus

### Production Setup

```bash
docker-compose -f docker-compose.prod.yml up
```

## 🔧 Configuration

### Development Configuration (`application-dev.yml`)

- **Server Port**: 8100
- **CORS**: Enabled for localhost:3000
- **JWT Access Token**: 360000ms (6 minutes)
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics

### Production Configuration (`application-prod.yml`)

- **Jaeger Tracing**: 10% sampling
- **Enhanced Security**: Production-grade settings
- **Optimized Performance**: Production-optimized configurations

## 📡 API Endpoints

### Authentication Endpoints

- `POST /login` - User authentication
- `POST /logout` - User logout
- `POST /refresh` - Token refresh

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

The service includes Kubernetes manifests in the `k8s/` directory:

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/
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
