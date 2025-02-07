# Stage 1: Build the jar using Maven
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:11-jre-slim
WORKDIR /app
# Copy the built jar from the previous stage
COPY --from=build /app/target/flink-sensor-data.jar /app/flink-sensor-data.jar
CMD ["bash"]
