FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S application-service && adduser -S application-service -G application-service

COPY --from=build --chown=application-service:application-service /app/target/*.jar  app.jar

USER application-service:application-service

EXPOSE 8085

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

LABEL authors="sylvester onah"

ENTRYPOINT ["sh", "-c" ,"exec java ${JAVA_OPTS} -jar app.jar"]