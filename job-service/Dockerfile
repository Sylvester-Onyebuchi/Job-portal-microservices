FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S job-service && adduser -S job-service -G job-service

COPY --from=build --chown=job-service:job-service /app/target/*.jar app.jar

USER job-service:job-service

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8087

LABEL authors="sylvester onah"

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]