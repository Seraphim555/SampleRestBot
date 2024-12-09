FROM maven:3.9 AS build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim

COPY --from=build /target/*.jar app.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app.jar"]
