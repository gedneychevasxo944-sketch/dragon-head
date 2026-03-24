package org.dragon.observer.collector.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ObservationDataset 观测数据集
 * 统一封装针对 Character/Workspace/Memory/Skill 的所有观测数据
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationDataset {

    /**
     * 观测时间范围 - 开始
     */
    private LocalDateTime startTime;

    /**
     * 观测时间范围 - 结束
     */
    private LocalDateTime endTime;

    /**
     * Character 观测快照
     */
    private CharacterObservationSnapshot characterSnapshot;

    /**
     * Workspace 观测快照
     */
    private WorkspaceObservationSnapshot workspaceSnapshot;

    /**
     * Memory 观测快照
     */
    private MemoryObservationSnapshot memorySnapshot;

    /**
     * Skill 观测快照
     */
    private SkillObservationSnapshot skillSnapshot;

    /**
     * 原始任务数据引用
     */
    private List<Map<String, Object>> rawTaskData;

    /**
     * 扩展数据
     */
    private Map<String, Object> extensions;
}
