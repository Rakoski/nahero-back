FROM eclipse-temurin:21-jdk-jammy AS build
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ ./src/

RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r naherouser && \
    useradd -r -g naherouser -m -d /home/naherouser naherouser && \
    mkdir -p /app && \
    chown -R naherouser:naherouser /app

COPY --from=build /app/target/*.jar app.jar
COPY keystore.p12 /app/keystore.p12
RUN chown naherouser:naherouser /app/app.jar

USER naherouser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f https://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
