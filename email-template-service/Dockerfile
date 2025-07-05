# === Stage 1: Build the application ===
FROM --platform=linux/amd64 maven:3.9.9-amazoncorretto-23-alpine AS build
WORKDIR /app

# Accept build arguments for GitHub credentials
ARG GITHUB_TOKEN
ARG GITHUB_USERNAME

# Copy the entire project (including all modules)
COPY . .

# Copy and execute the Maven settings generation script
COPY generate-maven-settings.sh /tmp/
RUN chmod +x /tmp/generate-maven-settings.sh && \
    /tmp/generate-maven-settings.sh "$GITHUB_USERNAME" "$GITHUB_TOKEN"

# Build only the email-template-service-web module and its dependencies
RUN mvn clean package -pl email-template-service-web -am -DskipTests

# === Stage 2: Create the runtime image ===
FROM --platform=linux/amd64 amazoncorretto:23-alpine-jdk
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/email-template-service-web/target/email-template-service-web-*.jar app.jar

# Expose the application port (defined in application-dev.yaml)
EXPOSE 8091

# Run the application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5008", "-jar", "-Dmicronaut.environments=${MICRONAUT_ENVIRONMENTS}", "app.jar"] 
