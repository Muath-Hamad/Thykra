# syntax=docker/dockerfile:1.7

# ---- Build stage --------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src

# Copy the wrapper + version manifest first so Gradle's daemon-less init
# benefits from layer caching when source changes but dependencies don't.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared/build.gradle.kts ./shared/
COPY server/build.gradle.kts ./server/

# Now copy sources for the modules we actually need to build the server.
# composeApp / iosApp / webApp are intentionally excluded — see .dockerignore.
COPY shared ./shared
COPY server ./server

RUN chmod +x gradlew

# Use BuildKit cache mounts so Gradle's caches survive across image rebuilds.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :server:installDist --no-daemon --stacktrace

# ---- Runtime stage ------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl is needed by HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user.
RUN groupadd --system thykra && useradd --system --gid thykra --create-home thykra

WORKDIR /app
COPY --from=build /src/server/build/install/server ./

# Writable data dir for local-storage fallback in test-prod environments;
# real prod uses S3 (STORAGE_TYPE=s3) so this stays empty.
RUN mkdir -p /app/data && chown -R thykra:thykra /app

USER thykra
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:8081/api/health || exit 1

ENTRYPOINT ["/app/bin/server"]
