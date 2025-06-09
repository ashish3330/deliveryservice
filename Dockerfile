# Use official OpenJDK as base image
FROM openjdk:17-jdk-slim

# Set runtime environment variable
ENV SPRING_PROFILES_ACTIVE=prod

# Set the working directory
WORKDIR /app
# Copy the built JAR file
COPY target/product-service.jar product-service.jar

# Run the application with the correct Spring profile
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "product-service.jar"]
