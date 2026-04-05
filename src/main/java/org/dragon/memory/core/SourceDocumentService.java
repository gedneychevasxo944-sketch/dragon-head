package org.dragon.memory.core;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.SourceDocumentDTO;
import org.dragon.api.controller.dto.memory.CreateSourceRequest;
import org.dragon.api.controller.dto.memory.UpdateSourceRequest;

/**
 * 数据源服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface SourceDocumentService {
    /**
     * 获取数据源列表
     *
     * @param search     搜索关键词
     * @param status     状态过滤
     * @param sourceType 源类型过滤
     * @param page       页码
     * @param pageSize   每页大小
     * @return 数据源分页列表
     */
    PageResponse<SourceDocumentDTO> getSources(String search, String status, String sourceType, int page, int pageSize);

    /**
     * 获取数据源详情
     *
     * @param sourceId 数据源 ID
     * @return 数据源详情
     */
    SourceDocumentDTO getSource(String sourceId);

    /**
     * 创建数据源
     *
     * @param request 创建数据源请求
     * @return 已创建的数据源
     */
    SourceDocumentDTO createSource(CreateSourceRequest request);

    /**
     * 更新数据源
     *
     * @param sourceId 数据源 ID
     * @param request 更新数据源请求
     * @return 已更新的数据源
     */
    SourceDocumentDTO updateSource(String sourceId, UpdateSourceRequest request);

    /**
     * 删除数据源
     *
     * @param sourceId 数据源 ID
     * @return 是否成功
     */
    boolean deleteSource(String sourceId);

    /**
     * 同步数据源
     *
     * @param sourceId 数据源 ID
     * @return 同步结果
     */
    String syncSource(String sourceId);
}
