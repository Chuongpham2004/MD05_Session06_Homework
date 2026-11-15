# Sử dụng image JRE nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR đã được build (./gradlew bootJar) vào container
COPY build/libs/*.jar app.jar

# transaction-service lắng nghe ở cổng 8081
EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
