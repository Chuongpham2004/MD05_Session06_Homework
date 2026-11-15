# Sử dụng image JRE nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR đã được build (./gradlew bootJar) vào container
COPY build/libs/*.jar app.jar

# Gateway lắng nghe 8080 trong container, compose map ra cổng 80 của host
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
