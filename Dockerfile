# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn -q -DskipTests package && \
    cp target/resplite-1.0.0-SNAPSHOT.jar /build/resplite.jar

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/resplite.jar /app/resplite.jar
RUN mkdir -p /data
ENV RESPLITE_DATA_DIR=/data
ENV RESPLITE_PORT=6380
EXPOSE 6380 6381
ENTRYPOINT ["java", "-jar", "/app/resplite.jar"]