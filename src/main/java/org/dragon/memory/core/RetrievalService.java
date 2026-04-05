package org.dragon.memory.core;

import org.dragon.api.controller.dto.RetrievalRequestDTO;
import org.dragon.api.controller.dto.RetrievalResponseDTO;

/**
 * 检索服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface RetrievalService {
    /**
     * 检索记忆内容
     *
     * @param request 检索请求
     * @return 检索响应
     */
    RetrievalResponseDTO search(RetrievalRequestDTO request);

    /**
     * 检索测试
     *
     * @param request 检索请求
     * @return 检索响应
     */
    RetrievalResponseDTO testRetrieval(RetrievalRequestDTO request);
}
