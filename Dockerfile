# Sử dụng image JRE nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR đã được build vào container
COPY build/libs/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Lệnh khởi chạy ứng dụng
CMD ["java", "-jar", "app.jar"]