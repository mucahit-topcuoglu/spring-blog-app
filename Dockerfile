# Use official Gradle image with JDK 21
FROM gradle:8.5-jdk21 AS build

# Set working directory
WORKDIR /home/gradle/project

# Copy all project files
COPY --chown=gradle:gradle . .

# Build the application
RUN gradle build -x test --no-daemon

# List the generated jar files for debugging
RUN ls -la /home/gradle/project/build/libs/

# Use JRE 21 for runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage with specific name
COPY --from=build /home/gradle/project/build/libs/blog-projesi-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
