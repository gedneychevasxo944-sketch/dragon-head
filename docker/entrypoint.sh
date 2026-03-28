#!/bin/sh
# ===================================================================
# DragonHead Docker Entrypoint
# 1. 等待 MySQL 就绪
# 2. 执行 Flyway 迁移（如果启用）
# 3. 启动 Java 应用
# ===================================================================

set -e

echo "[Entrypoint] 等待 MySQL 启动..."
# 等待 MySQL 可用（支持 docker-compose depends_on 的健康检查）
if [ -n "$MYSQL_HOST" ]; then
    MYSQL_PORT=${MYSQL_PORT:-3306}
    until mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e 'SELECT 1' > /dev/null 2>&1; do
        echo "[Entrypoint] MySQL 不可用，等待 3 秒..."
        sleep 3
    done
    echo "[Entrypoint] MySQL 已就绪"
fi

echo "[Entrypoint] 启动 DragonHead 应用..."
exec java -XX:+UseContainerSupport \
         -XX:MaxRAMPercentage=75.0 \
         -Djava.security.egd=file:/dev/./urandom \
         -jar /app/app.jar \
         "$@"
