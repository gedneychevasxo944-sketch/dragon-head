#!/bin/bash
# ===================================================================
# entrypoint-dev.sh - 开发环境启动脚本
# 支持热挂载：检测 classes 目录变化后自动重启 Java 进程
# ===================================================================

APP_DIR="/app"
CLASSES_DIR="${APP_DIR}/classes"
MAX_WAIT=120
INTERVAL=2

echo "[entrypoint-dev] 等待 MySQL 就绪..."

# 等待 MySQL
until mysqladmin ping -h mysql -u root -p"${MYSQL_ROOT_PASSWORD:-123456}" --silent 2>/dev/null; do
    echo "[entrypoint-dev] 等待 MySQL 启动..."
    sleep 2
done
echo "[entrypoint-dev] MySQL 已就绪"

# 启动 JVM（前台运行，PID 记录在文件）
start_app() {
    echo "[entrypoint-dev] 启动应用..."
    java ${JAVA_OPTS} -cp "${CLASSES_DIR}" org.dragon.DragonHeadApplication
}

# 检测 classpath 变化
watch_and_restart() {
    local pid=$$

    # 第一次启动
    start_app &
    local app_pid=$!

    echo "[entrypoint-dev] 应用 PID: ${app_pid}"

    # 监听 SIGTERM（docker stop 时优雅关闭）
    trap "echo '[entrypoint-dev] 收到停止信号...'; kill -TERM ${app_pid} 2>/dev/null; wait ${app_pid}; exit 0" SIGTERM SIGINT

    # 每 3 秒检查 classes 修改时间
    local last_mtime=$(stat -c %Y "${CLASSES_DIR}" 2>/dev/null || echo 0)

    while kill -0 ${app_pid} 2>/dev/null; do
        sleep 3
        local new_mtime=$(stat -c %Y "${CLASSES_DIR}" 2>/dev/null || echo 0)
        if [ "$new_mtime" != "$last_mtime" ] && [ "$new_mtime" != "0" ]; then
            echo "[entrypoint-dev] 检测到 classes 变化，重新启动..."
            last_mtime=$new_mtime
            kill -TERM ${app_pid} 2>/dev/null
            wait ${app_pid} 2>/dev/null
            sleep 1
            start_app &
            app_pid=$!
            echo "[entrypoint-dev] 新应用 PID: ${app_pid}"
        fi
    done

    wait ${app_pid}
}

watch_and_restart
