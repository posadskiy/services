# === Stage 1: Build the application ===
FROM maven:3.9.9-amazoncorretto-23-alpine AS build
WORKDIR /app

# Copy the entire project (including all modules)
COPY . .

# Build only the email-service-web module and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl email-service-web -am -DskipTests 

# === Stage 2: Create the runtime image ===
FROM amazoncorretto:23-alpine-jdk
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/email-service-web/target/email-service-web-*.jar app.jar

# Expose the application port (defined in application-dev.yml)
EXPOSE 8090

# Run the application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007", "-jar", "-Dmicronaut.environments=${MICRONAUT_ENVIRONMENTS}", "app.jar"] 