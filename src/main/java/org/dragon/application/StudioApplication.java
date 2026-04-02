package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.workspace.WorkspaceApplicationProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * StudioApplication Studio 模块应用服务
 *
 * <p>对应前端 /studio 页面，聚合 Character、Trait、Template、Deployment 相关业务逻辑。
 * Controller 层直接调用本服务，不直接调用底层 Service/Registry。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioApplication {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceApplicationProvider workspaceApplicationProvider;

    // ==================== Character CRUD ====================

    /**
     * 分页获取角色列表，支持状态/搜索筛选。
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页数量
     * @param search   名称/描述搜索关键词
     * @param status   状态筛选（null 表示全部）
     * @param source   来源筛选（存储在 extensions.source，null 表示全部）
     * @return 分页结果
     */
    public PageResponse<Character> listCharacters(int page, int pageSize, String search,
                                                  String status, String source) {
        List<Character> all = characterRegistry.listAll();

        // 过滤
        List<Character> filtered = all.stream()
                .filter(c -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = c.getName() != null && c.getName().toLowerCase().contains(s);
                        boolean descMatch = c.getDescription() != null && c.getDescription().toLowerCase().contains(s);
                        boolean idMatch = c.getId() != null && c.getId().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch && !idMatch) return false;
                    }
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        CharacterProfile.Status st = parseStatus(status);
                        if (st != null && c.getStatus() != st) return false;
                    }
                    if (source != null && !source.isBlank() && !"all".equalsIgnoreCase(source)) {
                        Object ext = c.getExtensions() != null ? c.getExtensions().get("source") : null;
                        if (!source.equalsIgnoreCase(String.valueOf(ext))) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Character> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);

        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 创建角色。
     *
     * @param character 角色实体
     * @return 创建后的角色
     */
    public Character createCharacter(Character character) {
        characterRegistry.register(character);
        log.info("[StudioApplication] Created character: {}", character.getId());
        return characterRegistry.get(character.getId()).orElse(character);
    }

    /**
     * 获取角色详情。
     *
     * @param characterId 角色 ID
     * @return 角色
     */
    public Optional<Character> getCharacter(String characterId) {
        return characterRegistry.get(characterId);
    }

    /**
     * 更新角色信息。
     *
     * @param characterId 角色 ID
     * @param character   更新内容
     * @return 更新后的角色
     */
    public Character updateCharacter(String characterId, Character character) {
        character.setId(characterId);
        characterRegistry.update(character);
        log.info("[StudioApplication] Updated character: {}", characterId);
        return characterRegistry.get(characterId).orElse(character);
    }

    /**
     * 删除（注销）角色。
     *
     * @param characterId 角色 ID
     */
    public void deleteCharacter(String characterId) {
        characterRegistry.unregister(characterId);
        log.info("[StudioApplication] Deleted character: {}", characterId);
    }

    /**
     * 获取 Studio 统计数据。
     *
     * @return 统计信息 Map
     */
    public Map<String, Object> getStudioStats() {
        List<Character> all = characterRegistry.listAll();
        long totalCharacters = all.size();
        long activeCharacters = all.stream()
                .filter(c -> c.getStatus() == CharacterProfile.Status.RUNNING
                        || c.getStatus() == CharacterProfile.Status.LOADED)
                .count();
        long runningCharacters = all.stream()
                .filter(c -> c.getStatus() == CharacterProfile.Status.RUNNING)
                .count();

        return Map.of(
                "totalCharacters", totalCharacters,
                "activeCharacters", activeCharacters,
                "runningCharacters", runningCharacters,
                "totalTraits", 0,           // Trait 系统待实现
                "totalDeployments", 0        // Deployment 系统待实现
        );
    }

    /**
     * 独立运行角色（向指定角色发送消息，用于 Studio 测试页面）。
     *
     * @param characterId 角色 ID
     * @param message     用户消息
     * @param sessionId   会话 ID（可选）
     * @return 角色回复
     */
    public Map<String, Object> runCharacter(String characterId, String message, String sessionId) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        String reply = character.run(message);
        String sid = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString();

        return Map.of(
                "sessionId", sid,
                "reply", reply != null ? reply : "",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    // ==================== Deployment 管理 ====================

    /**
     * 获取派驻记录列表（Character 已部署到的 Workspace 映射）。
     *
     * @param page        页码
     * @param pageSize    每页数量
     * @param characterId 按角色 ID 筛选（可选）
     * @param workspaceId 按 Workspace ID 筛选（可选）
     * @return 分页派驻记录
     */
    public PageResponse<Map<String, Object>> listDeployments(int page, int pageSize,
                                                             String characterId, String workspaceId) {
        List<Character> all = characterRegistry.listAll();
        List<Map<String, Object>> deployments = all.stream()
                .filter(c -> characterId == null || characterId.equals(c.getId()))
                .flatMap(c -> {
                    List<String> wsIds = c.getWorkspaceIds();
                    if (wsIds == null) return java.util.stream.Stream.empty();
                    return wsIds.stream()
                            .filter(wid -> workspaceId == null || workspaceId.equals(wid))
                            .map(wid -> buildDeploymentRecord(c, wid));
                })
                .collect(Collectors.toList());

        long total = deployments.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, deployments.size());
        List<Map<String, Object>> pageData = fromIndex >= deployments.size() ? List.of()
                : deployments.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 派驻角色到 Workspace。
     *
     * @param characterId 角色 ID
     * @param workspaceId 目标 Workspace ID
     * @param role        职责
     * @param position    职位
     * @param level       级别
     * @return 派驻记录
     */
    public Map<String, Object> deployCharacter(String characterId, String workspaceId,
                                               String role, String position, Integer level) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // 添加 workspaceId 到 character 的 workspaceIds
        List<String> wsIds = character.getWorkspaceIds() != null
                ? new java.util.ArrayList<>(character.getWorkspaceIds())
                : new java.util.ArrayList<>();
        if (!wsIds.contains(workspaceId)) {
            wsIds.add(workspaceId);
        }
        character.setWorkspaceIds(wsIds);
        characterRegistry.update(character);

        // 同时加入 workspace 成员
        try {
            workspaceApplicationProvider.getApplication(workspaceId)
                    .addMember(workspaceId, characterId, role != null ? role : "member");
        } catch (Exception e) {
            log.warn("[StudioApplication] Failed to add member to workspace {}: {}", workspaceId, e.getMessage());
        }

        return Map.of(
                "id", characterId + "_" + workspaceId,
                "characterId", characterId,
                "characterName", character.getName() != null ? character.getName() : "",
                "workspaceId", workspaceId,
                "workspaceName", workspaceId,
                "role", role != null ? role : "",
                "position", position != null ? position : "",
                "level", level != null ? level : 3,
                "status", "idle",
                "deployedAt", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * 撤销派驻。
     *
     * @param deploymentId 格式为 characterId_workspaceId
     */
    public void undeployCharacter(String deploymentId) {
        String[] parts = deploymentId.split("_", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid deploymentId: " + deploymentId);
        }
        String charId = parts[0];
        String wsId = parts[1];

        Character character = characterRegistry.get(charId).orElse(null);
        if (character != null && character.getWorkspaceIds() != null) {
            List<String> wsIds = new java.util.ArrayList<>(character.getWorkspaceIds());
            wsIds.remove(wsId);
            character.setWorkspaceIds(wsIds);
            characterRegistry.update(character);
        }

        try {
            workspaceApplicationProvider.getApplication(wsId).removeMember(wsId, charId);
        } catch (Exception e) {
            log.warn("[StudioApplication] Failed to remove member from workspace {}: {}", wsId, e.getMessage());
        }

        log.info("[StudioApplication] Undeployed character {} from workspace {}", charId, wsId);
    }

    // ==================== 内部工具 ====================

    private Map<String, Object> buildDeploymentRecord(Character c, String wid) {
        java.util.HashMap<String, Object> record = new java.util.HashMap<>();
        record.put("id", c.getId() + "_" + wid);
        record.put("characterId", c.getId());
        record.put("characterName", c.getName() != null ? c.getName() : "");
        record.put("workspaceId", wid);
        record.put("workspaceName", wid);
        record.put("status", c.getStatus() != null ? c.getStatus().name().toLowerCase() : "idle");
        record.put("deployedAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        record.put("lastActiveAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        return record;
    }

    private CharacterProfile.Status parseStatus(String status) {
        try {
            return CharacterProfile.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
