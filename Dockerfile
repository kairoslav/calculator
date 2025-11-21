# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Cache the .m2 repository
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:resolve-plugins dependency:resolve # or mvn dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS=""

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
