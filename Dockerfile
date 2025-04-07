FROM eclipse-temurin:21-jdk-alpine as builder
# Set working directory
WORKDIR /app
# Copy all files (respecting .dockerignore)
COPY . .
# Accept GitHub credentials as build arguments
ARG GPR_USER
ARG GPR_KEY
# Create gradle.properties at build time with GitHub credentials
RUN echo "gpr.user=${GPR_USER}" > gradle.properties && \
    echo "gpr.key=${GPR_KEY}" >> gradle.properties
# Build the application
RUN ./gradlew clean build -x test

# Second stage to run only the jar
FROM eclipse-temurin:21-jre-alpine
# Set working directory
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/smart-content-*.jar app.jar
# Expose the port your Spring Boot app uses
EXPOSE 8080
# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]