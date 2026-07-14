FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build --chown=spring:spring /workspace/build/libs/keeply-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]
