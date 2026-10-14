# syntax=docker/dockerfile:1

# ---- Derleme aşaması ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Önce yalnızca derleme betikleri: Gradle dağıtımı ve bağımlılık çözümlemesi ayrı katmanda önbelleğe alınır
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
# Testler CI'da/yerelde çalışır (Testcontainers Docker gerektirir); imajda yalnızca derleme yapılır.
RUN ./gradlew --no-daemon -q bootJar -x test

# ---- Çalışma aşaması ----
FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
