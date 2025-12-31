# Use official Gradle image with JDK 21
FROM gradle:8.5-jdk21 AS build

# Set working directory
WORKDIR /home/gradle/project

# Copy all project files
COPY --chown=gradle:gradle . .

# Build the application
RUN gradle build -x test --no-daemon

# Use JRE 21 for runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage - use wildcard but reference correctly
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar

# Expose port
EXPOSE 8080

# Run the application with PORT environment variable support
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
