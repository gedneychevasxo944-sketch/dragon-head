package org.dragon.application;

import java.util.List;
import java.util.Map;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.workspace.DeploymentService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * StudioApplication Studio 模块应用服务
 *
 * <p>对应前端 /studio 页面，作为 facade 委托 CharacterTemplateService 和 DeploymentService 处理业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioApplication {

    private final DeploymentService deploymentService;

    // ==================== Deployment 管理 ====================

    /**
     * 获取派驻记录列表。
     */
    public PageResponse<Map<String, Object>> listDeployments(int page, int pageSize,
                                                             String characterId, String workspaceId) {
        List<Map<String, Object>> all = deploymentService.listDeployments(page, pageSize, characterId, workspaceId);
        long total = all.size();
        return PageResponse.of(all, total, page, pageSize);
    }

    /**
     * 派驻角色到 Workspace。
     */
    public Map<String, Object> deployCharacter(String characterId, String workspaceId,
                                               String role, String position, Integer level) {
        return deploymentService.deployCharacter(characterId, workspaceId, role, position, level);
    }

    /**
     * 撤销派驻。
     */
    public void undeployCharacter(String deploymentId) {
        deploymentService.undeployCharacter(deploymentId);
    }
}
