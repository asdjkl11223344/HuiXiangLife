FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY huixiang-common/pom.xml huixiang-common/pom.xml
COPY huixiang-pojo/pom.xml huixiang-pojo/pom.xml
COPY huixiang-server/pom.xml huixiang-server/pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/huixiang-server/target/huixiang-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

