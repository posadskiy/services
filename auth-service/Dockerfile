# === Stage 1: Build the application ===
FROM maven:3.9.12-amazoncorretto-25-alpine AS build
WORKDIR /app

# Accept build arguments for GitHub credentials
ARG GITHUB_TOKEN
ARG GITHUB_USERNAME

# Copy the entire project (including all modules)
COPY . .

# Build only the auth-service-web module and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mkdir -p /root/.m2 && \
    sed -e 's/\${GITHUB_USERNAME}/'$GITHUB_USERNAME'/g' \
        -e 's/\${GITHUB_TOKEN}/'$GITHUB_TOKEN'/g' \
        maven-settings.xml > /root/.m2/settings.xml && \
    mvn clean package -pl auth-service-web -am -DskipTests -s /root/.m2/settings.xml

# === Stage 2: Create the runtime image ===
FROM amazoncorretto:25-alpine-jdk
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/auth-service-web/target/auth-service-web-*.jar app.jar

# Expose the application port (defined in application.yaml)
EXPOSE 8100

# Run the application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-Dmicronaut.environments=${MICRONAUT_ENVIRONMENTS}", "-jar", "app.jar"]
