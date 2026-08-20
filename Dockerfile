FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Bootstrap gradle-wrapper.jar from the version declared in gradle-wrapper.properties
RUN apt-get update && apt-get install -y --no-install-recommends wget unzip \
    && GRADLE_VERSION=$(sed -n 's|.*distributions/gradle-\([^-]*\)-bin\.zip.*|\1|p' gradle/wrapper/gradle-wrapper.properties) \
    && wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /tmp/gradle-dist \
    && /tmp/gradle-dist/gradle-${GRADLE_VERSION}/bin/gradle wrapper \
    && rm -rf /tmp/gradle.zip /tmp/gradle-dist \
    && apt-get purge -y wget unzip && rm -rf /var/lib/apt/lists/*

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
