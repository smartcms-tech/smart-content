FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# Second stage to run only the jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built jar (update if the jar name is different)
COPY --from=builder /app/build/libs/smart-content-*.jar app.jar
# Expose the port your Spring Boot app uses
EXPOSE 8080
# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
