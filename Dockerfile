FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 构建机共享出口 IP，直连 Maven Central 会被 429 限流
COPY maven-settings.xml /root/.m2/settings.xml
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
ENV TZ=Asia/Shanghai
EXPOSE 80
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
