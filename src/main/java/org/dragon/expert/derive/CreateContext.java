package org.dragon.expert.derive;

import lombok.Builder;
import lombok.Data;
import org.dragon.permission.enums.ResourceType;

import java.util.Map;

/**
 * CreateContext 创建 Expert 上下文
 *
 * @author yijunw
 */
@Data
@Builder
public class CreateContext {

    /**
     * 资源类型
     */
    private ResourceType resourceType;

    /**
     * 资产名称
     */
    private String name;

    /**
     * 资产描述
     */
    private String description;

    /**
     * Expert 分类
     */
    private String category;

    /**
     * 预览文本
     */
    private String preview;

    /**
     * 目标用户群体
     */
    private String targetAudience;

    /**
     * 资产特定配置（Map 结构，内容因类型而异）
     */
    private Map<String, Object> config;
}