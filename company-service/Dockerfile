FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine as runtime

RUN addgroup -S company-service && adduser -S company-service -G company-service

COPY --from=build --chown=company-service:company-service /app/target/*.jar  app.jar

USER company-service:company-service

EXPOSE 8086

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"


LABEL authors="sylvester onah"

ENTRYPOINT ["sh", "-c","exec java ${JAVA_OPTS} -jar app.jar"]