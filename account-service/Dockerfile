# ===== Stage 1: build file JAR bằng Gradle wrapper (không cần cài JDK/Gradle trên máy host) =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copy cấu hình build trước để Docker cache lớp tải dependencies
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

# Copy source rồi build (bỏ qua test vì test cần DB đang chạy)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon -q

# ===== Stage 2: image chạy, chỉ chứa JRE + file JAR =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Chạy bằng user thường, không dùng root
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER app

# account-service lắng nghe cổng 8080 trong container (chỉ dùng nội bộ trong banking-network)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
