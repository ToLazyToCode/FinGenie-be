FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -r -u 1001 spring

COPY --from=build /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/logs \
    && chown -R spring:spring /app/logs

USER spring

EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
