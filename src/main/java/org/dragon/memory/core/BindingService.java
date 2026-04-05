package org.dragon.memory.core;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.BindingDTO;
import org.dragon.api.controller.dto.CreateBindingRequest;
import org.dragon.api.controller.dto.UpdateBindingRequest;

/**
 * 绑定关系服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface BindingService {
    /**
     * 获取绑定列表
     *
     * @param fileId     文件 ID（可选）
     * @param targetType 目标类型（可选）
     * @param targetId   目标 ID（可选）
     * @param page       页码
     * @param pageSize   每页大小
     * @return 绑定分页列表
     */
    PageResponse<BindingDTO> getBindings(String fileId, String targetType, String targetId, int page, int pageSize);

    /**
     * 获取绑定详情
     *
     * @param bindingId 绑定 ID
     * @return 绑定详情
     */
    BindingDTO getBinding(String bindingId);

    /**
     * 创建绑定
     *
     * @param request 创建绑定请求
     * @return 已创建的绑定
     */
    BindingDTO createBinding(CreateBindingRequest request);

    /**
     * 更新绑定
     *
     * @param bindingId 绑定 ID
     * @param request 更新绑定请求
     * @return 已更新的绑定
     */
    BindingDTO updateBinding(String bindingId, UpdateBindingRequest request);

    /**
     * 删除绑定
     *
     * @param bindingId 绑定 ID
     * @return 是否成功
     */
    boolean deleteBinding(String bindingId);
}
