# User Service

A Micronaut-based user management service with comprehensive user registration, authentication, and profile management capabilities.

## 🚀 Features

- **User Registration & Management**: Complete user lifecycle management
- **JWT-based Authentication**: Secure token-based authentication
- **Password Security**: Secure password hashing and validation
- **User Profile Management**: User data CRUD operations
- **Social Identity Linking**: Attach/detach external OAuth providers to user profiles
- **PostgreSQL Database**: Persistent storage with Flyway migrations
- **Distributed Tracing**: Jaeger integration for observability
- **OpenAPI/Swagger**: Complete API documentation
- **Prometheus Metrics**: Built-in monitoring and metrics
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Architecture

### Core Components

- **User Service**: Core user management functionality
- **Registration Service**: User registration and validation
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
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

**GDPR-Compliant User Deletion**:

**Primary Deletion Endpoint** - `DELETE /v0/user/{id}`:
- GDPR-compliant deletion that always anonymizes user data (GDPR Article 17 - Right to Erasure)
- **Always anonymizes by default** (no configuration needed):
  - Replaces email with `deleted_user_{id}@deleted.local`
  - Replaces username with `deleted_user_{id}`
  - Removes password hash, picture URL, and other PII
  - Deletes all external identity data (OAuth providers)
  - Sets `active = FALSE` and `deactivated_at` timestamp
  - Preserves data structure for analytics while removing PII
- This is the primary and recommended endpoint for user deletion
- GDPR-compliant by default - anonymization preserves data structure while removing all PII

**Hard Delete Endpoint** - `POST /v0/user/{id}/hard-delete`:
- Alternative endpoint for complete data removal (no anonymization)
- Completely removes user and all related data from database
- Use only when hard delete is explicitly required
- May break referential integrity if other tables reference this user

**Important**: The default anonymization behavior is GDPR-compliant and recommended. It removes all personally identifiable information while preserving data structure for business needs (analytics, fraud prevention). See [GDPR_COMPLIANCE.md](./GDPR_COMPLIANCE.md) for detailed compliance information.

## 🛠️ Setup & Installation

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+

### Environment Variables

Create a `.env` file in the user-service directory:

```bash
# Database Configuration
USER_DATABASE_NAME=user_db
USER_DATABASE_USER=user_user
USER_DATABASE_PASSWORD=your_secure_password

# JWT Configuration
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here

# GitHub Packages (for Maven dependencies)
GITHUB_USERNAME=your_github_username
GITHUB_TOKEN=your_github_token
```

### Development Setup

1. **Clone and Navigate**:
   ```bash
   cd user-service
   ```

2. **Start with Docker Compose**:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

3. **Access the Service**:
   - Service: http://localhost:8095
- Swagger UI: http://localhost:8095/swagger-ui/index.html
- Prometheus Metrics: http://localhost:8095/prometheus

### Production Setup

```bash
docker-compose -f docker-compose.prod.yml up
```

## 🔧 Configuration

### Development Configuration (`application-dev.yml`)

- **Server Port**: 8300
- **CORS**: Enabled for localhost:3000
- **JWT Access Token**: 360000ms (6 minutes)
- **Jaeger Tracing**: 100% sampling
- **Prometheus**: Enabled with detailed metrics

### Production Configuration (`application-prod.yml`)

- **Jaeger Tracing**: 10% sampling
- **Enhanced Security**: Production-grade settings
- **Optimized Performance**: Production-optimized configurations

## 📡 API Endpoints

### User Management Endpoints

- `POST /signup` - User registration
- `GET /v0/user/{id}` - Get user by ID (returns 404 if user is deactivated)
- `PUT /v0/user/{id}` - Update user username (only active users can be updated)
- `DELETE /v0/user/{id}` - GDPR-compliant user deletion (always anonymizes data by default)
  - Always anonymizes user data (removes PII, preserves structure for analytics)
  - GDPR-compliant default behavior
- `POST /v0/user/{id}/hard-delete` - Hard delete endpoint (complete data removal without anonymization)
  - Use only when complete removal is explicitly required
  - May break referential integrity

### Social Identity Endpoints

- `GET /v0/user/{id}/identities` - List linked OAuth providers for a user
- `POST /v0/user/{id}/identities` - Link a provider to the user (used by auth-service sync jobs)
- `DELETE /v0/user/{id}/identities/{provider}` - Soft-remove a provider link

### Authentication Endpoints

- `POST /auth/login` - User authentication
- `POST /auth/logout` - User logout
- `POST /auth/refresh` - Token refresh

### Documentation

- `GET /swagger-ui/index.html` - Swagger UI
- `GET /swagger/user-service-0.0.yml` - OpenAPI specification

## 🔍 Monitoring & Observability

### Jaeger Tracing

The service is configured to send distributed traces to Jaeger:

- **Development**: 100% sampling rate
- **Production**: 10% sampling rate
- **Jaeger UI**: http://localhost:16686

### Prometheus Metrics

Built-in metrics include:
- User management metrics
- Authentication metrics
- HTTP request metrics
- JVM metrics

Access metrics at: `http://localhost:8095/prometheus`

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=UserServiceTest

# Integration tests
mvn test -Dtest=UserIntegrationTest
```

### Test Coverage

The service includes comprehensive tests for:
- **API DTOs**: UserDto serialization/deserialization
- **Core Services**: UserService, RegistrationService with proper mocking
- **Mappers**: UserEntityMapper, UserDtoMapper
- **Models**: User record, UserEntity
- **Utilities**: PasswordEncoder with BCrypt
- **Exceptions**: AuthException, ErrorMessage
- **Web Controllers**: UserController, RegistrationController
- **Integration Tests**: HTTP endpoint testing
- **Authentication**: Security filter testing
- **Swagger UI**: Documentation accessibility

### Test Results

- **Total Tests**: 17 tests across all modules
- **API Module**: 3 tests (UserDto serialization/deserialization)
- **Core Module**: 33 tests (services, mappers, models, exceptions, utilities)
- **Web Module**: 17 tests (controllers, integration, authentication, Swagger UI)

### Test Quality

- **Unit Tests**: Isolated testing with proper mocking
- **Integration Tests**: End-to-end HTTP endpoint testing
- **Exception Testing**: Comprehensive error scenario coverage
- **Serialization Testing**: DTO validation and transformation
- **Security Testing**: Authentication flow validation

## 🐳 Docker Deployment

### Development Build

```bash
docker build -t user-service:dev .
```

### Production Build

```bash
docker build -f Dockerfile.prod -t user-service:prod .
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
- **Access Control**: Role-based user access

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
