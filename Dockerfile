FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy dependency descriptors first for layer caching
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src/ src/
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
