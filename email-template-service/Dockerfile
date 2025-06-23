# === Stage 1: Build the application ===
FROM maven:3.9.9-amazoncorretto-23-alpine AS build
WORKDIR /app

# Copy the entire project (including all modules)
COPY . .

# Build only the template-web module and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl email-template-service-web -am -DskipTests 

# === Stage 2: Create the runtime image ===
FROM amazoncorretto:23-alpine-jdk
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/email-template-service-web/target/email-template-service-web-*.jar app.jar

# Expose the application port (defined in application-dev.yaml)
EXPOSE 8091

# Run the application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5008", "-jar", "-Dmicronaut.environments=${MICRONAUT_ENVIRONMENTS}", "app.jar"] 
