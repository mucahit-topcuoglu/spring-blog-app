# Use official Gradle image with JDK 17
FROM gradle:8.5-jdk17 AS build

# Set working directory
WORKDIR /home/gradle/project

# Copy all project files
COPY --chown=gradle:gradle . .

# Build the application
RUN gradle build -x test --no-daemon

# Use JRE for runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
