# syntax=docker/dockerfile:1.7
# Imagen única parametrizada para los 4 servicios Spring Boot (ARG MODULE).

# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace

# Primero solo los pom.xml (capa cacheable), luego el código.
COPY pom.xml ./
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY ticket-service/pom.xml ticket-service/
COPY api-gateway/pom.xml api-gateway/
COPY common/src common/src
COPY ${MODULE}/src ${MODULE}/src

# Caché de dependencias Maven entre builds (BuildKit)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am -DskipTests package

# ---------- Etapa 2: ejecución (JRE mínimo, usuario sin privilegios) ----------
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}.jar app.jar
USER app
EXPOSE 8080 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=3s --start-period=90s --retries=6 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
