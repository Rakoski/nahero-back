FROM eclipse-temurin:23 AS build
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ ./src/
RUN mvn package -DskipTests

FROM eclipse-temurin:23-jre-slim
WORKDIR /app

RUN useradd -m naherouser && \
    mkdir -p /app && \
    chown -R naherouser:naherouser /app

COPY --from=build /app/target/*.jar app.jar
RUN chown naherouser:naherouser /app/app.jar

USER naherouser

ENV SPRING_PROFILES_ACTIVE=production

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]