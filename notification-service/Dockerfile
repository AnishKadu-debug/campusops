# ---------- Build stage ----------
FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# Normalize Windows line endings so the wrapper runs on Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Best-effort dependency pre-fetch for layer caching
RUN ./mvnw -q -B dependency:go-offline || true

COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:26-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
