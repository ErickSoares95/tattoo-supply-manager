# Build stage. pom.xml first and dependencies resolved on their own layer: a code-only change
# reuses that layer instead of downloading the dependencies again (I1). Not offline: go-offline
# misses a few transitive artifacts, which the package step fetches.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime stage: JRE only, pinned tag, non-root user.
FROM eclipse-temurin:21.0.4_7-jre
WORKDIR /app

RUN useradd --system --uid 1001 --no-create-home app
COPY --from=build --chown=app /app/target/*.jar app.jar
USER app

EXPOSE 8080

# Heap capped as a share of the container limit (Render free tier: 512 MB). Kept low on purpose:
# the app already uses ~250 MB outside the heap (metaspace, JIT, threads - Spring, Hibernate,
# Spring AI), so 75% (384 MB heap) got the deploy OOM-killed. 30% = ~150 MB, close to the JVM's
# 25% default that ran fine, but explicit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=30 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
