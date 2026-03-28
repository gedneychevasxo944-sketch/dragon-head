# ===================================================================
# Stage 1: Build
# ===================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 拷贝 pom.xml 先下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 拷贝源码并构建
COPY src ./src
RUN mvn package -DskipTests -B

# ===================================================================
# Stage 2: Runtime
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

# 安全运行用户
RUN addgroup -S dragon && adduser -S dragon -G dragon && \
    apk add --no-cache curl netcat-openbsd

WORKDIR /app

# 拷贝构建产物
COPY --from=builder /app/target/*.jar app.jar

# 拷贝 entrypoint
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 数据目录
RUN mkdir -p /data/skills /tmp/agent-sandbox && chown -R dragon:dragon /data /tmp/agent-sandbox

USER dragon

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
