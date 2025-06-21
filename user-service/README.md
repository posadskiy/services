# User Service

A Micronaut-based user service with distributed tracing using Jaeger.

## Features

- User registration and management
- JWT-based authentication
- PostgreSQL database with Flyway migrations
- Distributed tracing with Jaeger
- OpenAPI/Swagger documentation

## Tracing Setup

This service is configured to send distributed traces to Jaeger. The tracing configuration includes:

### Development Environment
- Jaeger tracing enabled with 100% sampling
- HTTP sender configured to send traces to `http://jaeger:14268/api/traces`
- Connected to external `observability-stack` network

### Production Environment
- Jaeger tracing enabled with 10% sampling
- HTTP sender configured to send traces to `http://jaeger:14268/api/traces`
- Jaeger service included in the Docker Compose stack

## Running the Service

### Development
```bash
docker-compose -f docker-compose.dev.yml up
```

### Production
```bash
docker-compose -f docker-compose.prod.yml up
```

## Accessing Jaeger UI

Once the service is running, you can access the Jaeger UI at:
- Development: `http://localhost:16686` (if Jaeger is running in the observability stack)
- Production: `http://localhost:16686`

## Testing Tracing

To verify that tracing is working:

1. Start the service with Jaeger
2. Make requests to the API endpoints (e.g., `/signup`, `/v0/user/{id}`)
3. Check the Jaeger UI for traces
4. Run the tracing configuration test: `mvn test -Dtest=TracingConfigurationTest`

## API Documentation

Swagger UI is available at `/swagger-ui/index.html` when the service is running.
