-- V103: 插入测试数据（haha 用户）

-- 插入资产成员记录（给 haha 用户赋予技能 OWNER 权限）
INSERT INTO asset_member (resource_type, resource_id, user_id, role, accepted) VALUES ('SKILL', '1', 1, 'OWNER', 1);

-- 插入技能
INSERT INTO skill (id, name, introduction, category, visibility, builtin, tags, creator_type, creator_id, creator_name, status, created_at) VALUES
('1', '代码审查', '自动审查代码中的潜在问题并提供优化建议', 'coder', 'public', 0, '["代码审查", "质量", "自动化"]', 'personal', 1, 'haha', 'draft', NOW());

-- 插入技能版本 v1 (草稿)
INSERT INTO skill_version (skill_id, version, name, description, content, frontmatter, runtime_config, editor_id, editor_name, status, storage_type, storage_info, created_at) VALUES
('1', 1, '代码审查', '自动审查代码中的潜在问题', '# Code Review Skill\n\n## When to Use\n当用户发送代码并要求审查时使用\n\n## Actions\n1. 读取代码\n2. 分析潜在问题\n3. 提供优化建议', 'name: 代码审查\ndescription: 自动审查代码中的潜在问题\naliases:\n  - review\n  - 代码审查\nwhenToUse: 当用户发送代码并要求审查时使用\nargumentHint: 待审查的代码内容\nallowedTools:\n  - Read\n  - Write\n  - Bash\nessort: standard\ncontext: fork', '{"aliases":["review","代码审查"],"whenToUse":"当用户发送代码并要求审查时使用","argumentHint":"待审查的代码内容","allowedTools":["Read","Write","Bash"],"effort":"standard","executionContext":"fork","disableModelInvocation":false,"userInvocable":true,"persist":true,"persistMode":"summary"}', 1, 'haha', 'DRAFT', 'local', '{"bucket":null,"basePath":null,"rootFilePath":"/data/skills/1/v1/SKILL.md","files":[{"path":"SKILL.md","size":2048,"type":"markdown"},{"path":"schema/input.json","size":256,"type":"json"}]}', NOW());

-- 插入技能版本 v2 (已发布)
INSERT INTO skill_version (skill_id, version, name, description, content, frontmatter, runtime_config, editor_id, editor_name, status, release_note, storage_type, storage_info, created_at, published_at) VALUES
('1', 2, '代码审查', '优化了审查逻辑，支持更多语言', '# Code Review Skill v2\n\n## When to Use\n当用户发送代码并要求审查时使用\n\n## Actions\n1. 读取代码\n2. 静态分析\n3. 风格检查\n4. 提供优化建议', 'name: 代码审查\ndescription: 优化了审查逻辑，支持更多语言\naliases:\n  - review\n  - 代码审查\nwhenToUse: 当用户发送代码并要求审查时使用\nargumentHint: 待审查的代码内容\nallowedTools:\n  - Read\n  - Write\n  - Bash\n  - Grep\neffort: thorough\ncontext: fork', '{"aliases":["review","代码审查"],"whenToUse":"当用户发送代码并要求审查时使用","argumentHint":"待审查的代码内容","allowedTools":["Read","Write","Bash","Grep"],"effort":"thorough","executionContext":"fork","disableModelInvocation":false,"userInvocable":true,"persist":true,"persistMode":"full"}', 1, 'haha', 'PUBLISHED', '支持多语言审查，优化了提示词', 'local', '{"bucket":null,"basePath":null,"rootFilePath":"/data/skills/1/v2/SKILL.md","files":[{"path":"SKILL.md","size":3072,"type":"markdown"},{"path":"schema/input.json","size":256,"type":"json"},{"path":"rules/security.json","size":512,"type":"json"}]}', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- 更新技能的发布版本ID和状态
UPDATE skill SET status = 'active', published_version_id = (SELECT id FROM (SELECT id FROM skill_version WHERE skill_id = '1' AND version = 2) AS t) WHERE id = '1';

-- 插入操作日志
INSERT INTO skill_action_log (id, skill_id, skill_name, action_type, operator_id, operator_name, version, detail, created_at) VALUES
('log-001-0001-0001-000000000001', '1', '代码审查', 'PUBLISH', 1, 'haha', 2, '{"releaseNote":"支持多语言审查，优化了提示词"}', DATE_SUB(NOW(), INTERVAL 1 DAY));
