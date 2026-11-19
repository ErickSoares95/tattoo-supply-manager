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

# Size the heap from the container's memory limit instead of the host's (Render free tier: 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
