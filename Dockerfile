#Start with a base image containing Java runtime
FROM openjdk:21-jdk-slim

# Add the application's jar to the image
COPY build/libs/booking-system-0.0.1-SNAPSHOT.jar booking-system-0.0.1-SNAPSHOT.jar

# execute the application
ENTRYPOINT ["java", "-jar", "booking-system-0.0.1-SNAPSHOT.jar"]

# Expose the application port
EXPOSE 8080