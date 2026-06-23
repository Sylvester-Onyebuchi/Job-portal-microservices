FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S notification && adduser -S notification -G notification

COPY --from=build --chown=notification:notification /app/target/*.jar app.jar

USER notification:notification

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8083

LABEL authors="sylvester onah"

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]