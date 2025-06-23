# Docker Compose Setup for Microservices

This repository contains four microservices that can be run together using Docker Compose.

## Services

1. **Auth Service** - Port 8100 (Debug: 5005)
2. **User Service** - Port 8095 (Debug: 5006)
3. **Email Service** - Port 8090 (Debug: 5007)
4. **Email Template Service** - Port 8091 (Debug: 5008)
5. **PostgreSQL Database** - Port 5432

## Prerequisites

- Docker and Docker Compose installed
- External networks: `user-web-network` and `observability-stack-network`

## Environment Variables

Create a `.env` file in the root directory with the following variables:

```bash
# Database Configuration
AUTH_DATABASE_NAME=auth_db
AUTH_DATABASE_USER=postgres
AUTH_DATABASE_PASSWORD=your_secure_password

# JWT Configuration
JWT_GENERATOR_SIGNATURE_SECRET=your_jwt_secret_key_here_make_it_long_and_secure

# GitHub Configuration (for Maven dependencies)
GITHUB_USERNAME=your_github_username
GITHUB_TOKEN=your_github_personal_access_token

# Email Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
EMAIL_PROTOCOL=smtp
EMAIL_AUTH=true
EMAIL_STARTTLS_ENABLE=true
EMAIL_DEBUG=true
```

## Setup External Networks

Before running the services, create the required external networks:

```bash
docker network create user-web-network
docker network create observability-stack-network
```

## Running the Services

1. **Start all services:**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **View logs:**
   ```bash
   docker-compose -f docker-compose.dev.yml logs -f
   ```

3. **Stop all services:**
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

4. **Rebuild and start:**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d --build
   ```

## Service Dependencies

- **Database** must be healthy before other services start
- **Auth Service** starts after database is ready
- **User Service** starts after auth service
- **Email Service** starts after auth service
- **Email Template Service** starts after both auth and email services

## Access Points

- **Auth Service API:** http://localhost:8100
- **User Service API:** http://localhost:8095
- **Email Service API:** http://localhost:8090
- **Email Template Service API:** http://localhost:8091
- **Jaeger UI:** http://localhost:16686
- **Database:** localhost:5432

## Debug Ports

Each service exposes a debug port for remote debugging:
- Auth Service: 5005
- User Service: 5006
- Email Service: 5007
- Email Template Service: 5008

## Health Checks

The database includes a health check to ensure it's ready before other services start. Services will wait for the database to be healthy before starting.

## Logging

All services use JSON logging with rotation:
- Max file size: 10MB
- Max files: 3
- Labels for Promtail integration

## Troubleshooting

1. **Service won't start:** Check if external networks exist
2. **Database connection issues:** Ensure database is healthy and environment variables are correct
3. **Build failures:** Check GitHub credentials and Maven cache
4. **Port conflicts:** Ensure ports are not already in use on your system 
