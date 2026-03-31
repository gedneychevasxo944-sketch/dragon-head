#!/bin/bash

# ========== 配置 ==========
BASE_URL="http://localhost:8080"
TOKEN=""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_step() { echo -e "\n${YELLOW}==> $1${NC}"; }
echo_success() { echo -e "${GREEN}✓ $1${NC}"; }
echo_error() { echo -e "${RED}✗ $1${NC}"; }

# ========== 1. 登录获取 Token ==========
echo_step "1. 登录获取 Token"
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/user/login/password" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"Test123456"}')
echo "响应: $LOGIN_RESP"

TOKEN=$(echo $LOGIN_RESP | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
    echo_error "登录失败"
    exit 1
fi
echo_success "Token 获取成功"

# ========== 2. 查询 Skill 列表 ==========
echo_step "2. 查询 Skill 列表 (GET /api/skills)"
curl -s -X GET "$BASE_URL/api/skills" -H "Authorization: Bearer $TOKEN"
echo

# ========== 3. 查询单个 Skill ==========
echo_step "3. 查询单个 Skill (GET /api/skills/{id})"
curl -s -X GET "$BASE_URL/api/skills/1" -H "Authorization: Bearer $TOKEN"
echo

# ========== 4. 创建 Skill (需要 SKILL.md) ==========
echo_step "4. 创建 Skill (带 SKILL.md)"

TEST_SKILL_DIR="/tmp/test-skill-$(date +%s)"
mkdir -p "$TEST_SKILL_DIR/test-hello"

# 创建 SKILL.md（必需）- 放在 skill 名称目录下
cat > "$TEST_SKILL_DIR/test-hello/SKILL.md" << 'EOF'
---
name: test-hello
description: A test skill for API testing
version: 1.0.0
---
# Test Hello Skill

## Commands
- hello: Say hello
EOF

# 创建脚本
mkdir -p "$TEST_SKILL_DIR/test-hello/scripts"
cat > "$TEST_SKILL_DIR/test-hello/scripts/hello.sh" << 'EOF'
#!/bin/bash
echo "Hello from skill!"
EOF

# 打包（打包时使用 skill 名称作为目录）
cd "$TEST_SKILL_DIR"
zip -r /tmp/test-skill.zip test-hello > /dev/null
cd -

DATA_JSON='{"name":"test-hello","description":"测试Skill","category":"GENERAL","visibility":"PUBLIC","creatorType":"PERSONAL"}'
echo "data: $DATA_JSON"

CREATE_RESP=$(curl -s -X POST "$BASE_URL/api/skills" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@/tmp/test-skill.zip;filename=test-hello.zip" \
    -F "data=$DATA_JSON;type=application/json")
echo "响应: $CREATE_RESP"

SKILL_ID=$(echo $CREATE_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "创建的 Skill ID: $SKILL_ID"

if [ -n "$SKILL_ID" ] && [ "$SKILL_ID" != "null" ]; then
    # ========== 5. 更新 Skill ==========
    echo_step "5. 更新 Skill (PUT /api/skills/$SKILL_ID)"
    UPDATE_DATA='{"description":"更新后的描述","version":"1.0.1"}'
    curl -s -X PUT "$BASE_URL/api/skills/$SKILL_ID" \
        -H "Authorization: Bearer $TOKEN" \
        -F "data=$UPDATE_DATA;type=application/json"
    echo

    # ========== 6. 禁用 Skill ==========
    echo_step "6. 禁用 Skill (POST /api/skills/$SKILL_ID/disable)"
    curl -s -X POST "$BASE_URL/api/skills/$SKILL_ID/disable" -H "Authorization: Bearer $TOKEN"
    echo

    # ========== 7. 启用 Skill ==========
    echo_step "7. 启用 Skill (POST /api/skills/$SKILL_ID/enable)"
    curl -s -X POST "$BASE_URL/api/skills/$SKILL_ID/enable" -H "Authorization: Bearer $TOKEN"
    echo

    # ========== 8. 删除 Skill ==========
    echo_step "8. 删除 Skill (DELETE /api/skills/$SKILL_ID)"
    curl -s -X DELETE "$BASE_URL/api/skills/$SKILL_ID" -H "Authorization: Bearer $TOKEN" -w "\nHTTP Status: %{http_code}\n"
fi

# 清理
rm -rf "$TEST_SKILL_DIR" /tmp/test-skill.zip

echo -e "\n${GREEN}===== 测试完成 =====${NC}"