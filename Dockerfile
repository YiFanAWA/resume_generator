FROM node:20-alpine AS frontend-build

WORKDIR /workspace
COPY frontend/package*.json frontend/
RUN cd frontend && npm ci

COPY frontend frontend
COPY src/main/resources src/main/resources
RUN cd frontend && npm run build

FROM maven:3.9-eclipse-temurin-11 AS backend-build

WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
COPY --from=frontend-build /workspace/src/main/resources/static/app src/main/resources/static/app
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:11-jre

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 5000

RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build /workspace/target/resume-portal-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
