FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S config-server && adduser -S config-server -G config-server

COPY - - from=build --chown=config-server:config-server /app/target/*.jar  app.jar

USER config-server:config-server

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

LABEL authors="sylvester onah"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", " exec java ${JAVA_OPTS} -jar app.jar"]