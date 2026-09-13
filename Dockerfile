# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY ci-settings.xml /usr/share/maven/conf/settings.xml
COPY pom.xml .
COPY src/ ./src/

COPY keystore.p12 /app/keystore.p12
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -Dmaven.artifact.threads=10 package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r naherouser && \
    useradd -r -g naherouser -m -d /home/naherouser naherouser && \
    mkdir -p /app && \
    chown -R naherouser:naherouser /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/keystore.p12 /app/keystore.p12
RUN chown naherouser:naherouser /app/app.jar && \
    chown naherouser:naherouser /app/keystore.p12
USER naherouser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f https://localhost:8080/actuator/health --insecure || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
