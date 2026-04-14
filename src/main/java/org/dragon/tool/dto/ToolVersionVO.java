package org.dragon.tool.dto;

import org.dragon.tool.enums.ToolVersionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ToolVersionVO — 工具版本列表项 VO。
 *
 * <p>用于版本列表接口，展示版本号、状态、创建信息和发版备注。
 *
 * @author yangpengfei
 * @version 1.0
 */
@Data
@Builder
public class ToolVersionVO {

    /** 版本号 */
    private Integer version;

    /** 版本状态 */
    private ToolVersionStatus versionStatus;

    /** 编辑者用户 ID */
    private Long editorId;

    /** 编辑者用户名 */
    private String editorName;

    /** 版本创建时间 */
    private LocalDateTime createdAt;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 发版备注 */
    private String releaseNote;
}
