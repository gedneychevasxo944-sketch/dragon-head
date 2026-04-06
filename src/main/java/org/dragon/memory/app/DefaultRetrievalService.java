package org.dragon.memory.app;

import org.dragon.api.controller.dto.memory.RetrievalRequestDTO;
import org.dragon.api.controller.dto.memory.RetrievalResponseDTO;
import org.dragon.memory.core.RetrievalService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检索服务实现类
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultRetrievalService implements RetrievalService {
    @Override
    public RetrievalResponseDTO search(RetrievalRequestDTO request) {
        // TODO: 实现检索逻辑
        return RetrievalResponseDTO.builder()
                .results(List.of())
                .total(0)
                .latency(0)
                .build();
    }

    @Override
    public RetrievalResponseDTO testRetrieval(RetrievalRequestDTO request) {
        // TODO: 实现检索测试逻辑
        return RetrievalResponseDTO.builder()
                .results(List.of())
                .total(0)
                .latency(0)
                .build();
    }
}
