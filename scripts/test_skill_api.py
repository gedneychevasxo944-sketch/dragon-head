#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SkillController API 集成测试脚本

测试步骤：
1. 登录获取 token
2. 测试技能列表接口
3. 测试创建技能接口
4. 测试获取技能详情
5. 测试更新技能
6. 测试发布技能
7. 测试禁用/启用技能
8. 测试删除技能

依赖安装：
    pip install requests

运行：
    python scripts/test_skill_api.py
"""

import requests
import json
import time
import sys
import os

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api/v1"
USER_API = f"{BASE_URL}/api/user"

# 测试账号（需要先在数据库中存在）
TEST_USERNAME = "admin"
TEST_PASSWORD = "admin123"

# 全局 token
AUTH_TOKEN = None
OPERATOR_ID = 1
OPERATOR_NAME = "test-admin"


class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'


def log_info(msg):
    print(f"{Colors.BLUE}[INFO]{Colors.END} {msg}")


def log_success(msg):
    print(f"{Colors.GREEN}[SUCCESS]{Colors.END} {msg}")


def log_error(msg):
    print(f"{Colors.RED}[ERROR]{Colors.END} {msg}")


def log_warn(msg):
    print(f"{Colors.YELLOW}[WARN]{Colors.END} {msg}")


def get_headers():
    """获取带认证的请求头"""
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
    if AUTH_TOKEN:
        headers["Authorization"] = f"Bearer {AUTH_TOKEN}"
    return headers


def login():
    """登录获取 token"""
    log_info(f"正在登录用户: {TEST_USERNAME}")

    url = f"{USER_API}/login/password"
    data = {
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD
    }

    try:
        response = requests.post(url, json=data, headers=get_headers(), timeout=10)
        result = response.json()

        if (result.get("code") == 0 or result.get("code") == 200) and result.get("data"):
            global AUTH_TOKEN
            AUTH_TOKEN = result["data"].get("accessToken")
            log_success(f"登录成功，获取 token: {AUTH_TOKEN[:20]}...")
            return True
        else:
            log_error(f"登录失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"登录请求失败: {e}")
        return False


def test_list_skills():
    """测试获取技能列表"""
    log_info("测试 GET /api/v1/skills - 获取技能列表")

    url = f"{API_BASE}/skills"
    params = {
        "page": 1,
        "pageSize": 10
    }

    try:
        response = requests.get(url, headers=get_headers(), params=params, timeout=10)
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            data = result.get("data", {})
            items = data.get("items", [])
            log_success(f"获取技能列表成功，共 {len(items)} 条记录")
            return items
        else:
            log_error(f"获取技能列表失败: {result.get('message')}")
            return []
    except Exception as e:
        log_error(f"请求失败: {e}")
        return []


def test_create_skill():
    """测试创建技能"""
    log_info("测试 POST /api/v1/skills - 创建技能")

    url = f"{API_BASE}/skills"
    skill_name = f"test-skill-{int(time.time())}"

    request_data = {
        "name": skill_name,
        "displayName": f"测试技能-{skill_name}",
        "description": "这是一个测试技能",
        "content": "# Test Skill\n\nThis is a test skill.",
        "category": "development",
        "visibility": "public",
        "executionContext": "inline"
    }

    try:
        headers = get_headers()
        del headers["Content-Type"]
        with open("/tmp/test-skill.zip", "rb") as f:
            response = requests.post(
                url,
                headers=headers,
                files={
                    "file": ("test-skill.zip", f, "application/zip"),
                    "data": (None, json.dumps(request_data), "application/json")
                },
                data={"operatorId": str(OPERATOR_ID), "operatorName": OPERATOR_NAME},
                timeout=30
            )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            skill_id = result.get("data", {}).get("skillId")
            log_success(f"创建技能成功，skillId: {skill_id}")
            return skill_id
        else:
            log_error(f"创建技能失败: {result.get('message')}")
            return None
    except Exception as e:
        log_error(f"请求失败: {e}")
        return None


def test_get_skill(skill_id):
    """测试获取技能详情"""
    log_info(f"测试 GET /api/v1/skills/{skill_id} - 获取技能详情")

    url = f"{API_BASE}/skills/{skill_id}"

    try:
        response = requests.get(url, headers=get_headers(), timeout=10)
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            data = result.get("data", {})
            log_success(f"获取技能详情成功: {data.get('name')}, status: {data.get('status')}")
            return data
        else:
            log_error(f"获取技能详情失败: {result.get('message')}")
            return None
    except Exception as e:
        log_error(f"请求失败: {e}")
        return None


def test_update_skill(skill_id):
    """测试更新技能"""
    log_info(f"测试 PUT /api/v1/skills/{skill_id} - 更新技能")

    url = f"{API_BASE}/skills/{skill_id}"

    request_data = {
        "displayName": f"更新后的测试技能-{int(time.time())}",
        "description": "更新后的描述"
    }

    try:
        headers = get_headers()
        del headers["Content-Type"]
        response = requests.put(
            url,
            headers=headers,
            files={
                "file": None,
                "data": (None, json.dumps(request_data), "application/json")
            },
            data={"operatorId": str(OPERATOR_ID), "operatorName": OPERATOR_NAME},
            timeout=30
        )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            log_success(f"更新技能成功")
            return True
        else:
            log_error(f"更新技能失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"请求失败: {e}")
        return False


def test_publish_skill(skill_id):
    """测试发布技能"""
    log_info(f"测试 POST /api/v1/skills/{skill_id}/publish - 发布技能")

    url = f"{API_BASE}/skills/{skill_id}/publish"

    payload = {
        "version": "1.0.0",
        "changelog": "首次发布"
    }

    try:
        response = requests.post(
            url,
            headers=get_headers(),
            json=payload,
            params={"operatorId": OPERATOR_ID},
            timeout=10
        )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            log_success(f"发布技能成功")
            return True
        else:
            log_error(f"发布技能失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"请求失败: {e}")
        return False


def test_disable_skill(skill_id):
    """测试禁用技能"""
    log_info(f"测试 POST /api/v1/skills/{skill_id}/disable - 禁用技能")

    url = f"{API_BASE}/skills/{skill_id}/disable"

    try:
        response = requests.post(
            url,
            headers=get_headers(),
            params={"operatorId": OPERATOR_ID},
            timeout=10
        )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            log_success(f"禁用技能成功")
            return True
        else:
            log_error(f"禁用技能失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"请求失败: {e}")
        return False


def test_enable_skill(skill_id):
    """测试启用技能"""
    log_info(f"测试 POST /api/v1/skills/{skill_id}/enable - 启用技能")

    url = f"{API_BASE}/skills/{skill_id}/enable"

    try:
        response = requests.post(
            url,
            headers=get_headers(),
            params={"operatorId": OPERATOR_ID},
            timeout=10
        )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            log_success(f"启用技能成功")
            return True
        else:
            log_error(f"启用技能失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"请求失败: {e}")
        return False


def test_delete_skill(skill_id):
    """测试删除技能"""
    log_info(f"测试 DELETE /api/v1/skills/{skill_id} - 删除技能")

    url = f"{API_BASE}/skills/{skill_id}"

    try:
        response = requests.delete(
            url,
            headers=get_headers(),
            params={"operatorId": OPERATOR_ID},
            timeout=10
        )
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            log_success(f"删除技能成功")
            return True
        else:
            log_error(f"删除技能失败: {result.get('message')}")
            return False
    except Exception as e:
        log_error(f"请求失败: {e}")
        return False


def test_list_versions(skill_id):
    """测试获取技能版本列表"""
    log_info(f"测试 GET /api/v1/skills/{skill_id}/versions - 获取版本列表")

    url = f"{API_BASE}/skills/{skill_id}/versions"

    try:
        response = requests.get(url, headers=get_headers(), timeout=10)
        result = response.json()

        if result.get("code") == 0 or result.get("code") == 200:
            versions = result.get("data", [])
            log_success(f"获取版本列表成功，共 {len(versions)} 个版本")
            return versions
        else:
            log_error(f"获取版本列表失败: {result.get('message')}")
            return []
    except Exception as e:
        log_error(f"请求失败: {e}")
        return []


def check_service():
    """检查服务是否可用"""
    log_info("检查服务状态...")

    try:
        # 尝试访问 swagger 或其他公开接口
        response = requests.get(f"{BASE_URL}/v3/api-docs", timeout=5)
        if response.status_code == 200:
            log_success("服务可访问")
            return True
    except:
        pass

    try:
        # 尝试访问登录接口
        response = requests.get(f"{USER_API}/login/password", timeout=5)
        log_success("服务可访问")
        return True
    except Exception as e:
        log_error(f"服务不可访问: {e}")
        return False


def run_tests():
    """运行所有测试"""
    print("\n" + "="*60)
    print("SkillController API 集成测试")
    print("="*60 + "\n")

    # 1. 检查服务
    if not check_service():
        log_error("服务不可用，请确保应用已启动")
        sys.exit(1)

    # 2. 登录
    if not login():
        log_error("登录失败，无法继续测试")
        sys.exit(1)

    print("\n" + "-"*60)
    print("开始测试 Skill CRUD 接口")
    print("-"*60 + "\n")

    # 3. 测试列表接口
    skills = test_list_skills()

    # 4. 测试创建技能
    skill_id = test_create_skill()

    if skill_id:
        # 5. 测试获取详情
        test_get_skill(skill_id)

        # 6. 测试更新
        test_update_skill(skill_id)

        # 7. 测试发布
        test_publish_skill(skill_id)

        # 8. 获取版本列表
        test_list_versions(skill_id)

        # 9. 测试禁用
        test_disable_skill(skill_id)

        # 10. 测试启用
        test_enable_skill(skill_id)

        # 11. 测试删除（可选，取消注释以执行）
        # test_delete_skill(skill_id)

    print("\n" + "="*60)
    print("测试完成")
    print("="*60 + "\n")

    # 再次列出技能
    print("\n最终技能列表:")
    test_list_skills()


if __name__ == "__main__":
    run_tests()
