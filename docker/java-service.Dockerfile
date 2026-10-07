# syntax=docker/dockerfile:1.7

FROM maven:3.9.12-eclipse-temurin-17-alpine AS build
ARG MODULE
WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY link-service/pom.xml link-service/pom.xml
RUN mvn -B -ntp -pl "${MODULE}" -am dependency:go-offline

COPY api-gateway/src api-gateway/src
COPY auth-service/src auth-service/src
COPY link-service/src link-service/src
RUN mvn -B -ntp -pl "${MODULE}" -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
ARG MODULE
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/data/avatars \
    && chown -R spring:spring /app
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar app.jar

USER 100:101
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
