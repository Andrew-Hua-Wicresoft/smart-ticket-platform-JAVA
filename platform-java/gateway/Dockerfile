FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY pom.xml pom.xml
COPY platform-java/gateway/pom.xml platform-java/gateway/pom.xml
COPY platform-java/platform-service/pom.xml platform-java/platform-service/pom.xml
COPY platform-java platform-java

RUN mvn -pl platform-java/gateway -am package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/platform-java/gateway/target/gateway-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
