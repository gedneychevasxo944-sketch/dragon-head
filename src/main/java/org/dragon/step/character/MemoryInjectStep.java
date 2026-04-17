package org.dragon.step.character;

import java.util.List;

import org.dragon.agent.react.ReActContext;
import org.dragon.memory.entity.MemoryQuery;
import org.dragon.memory.entity.MemorySearchResult;
import org.dragon.memory.service.core.MemoryFacade;
import org.dragon.step.StepResult;
import org.springframework.beans.factory.ObjectProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MemoryInjectStep - 记忆召回
 *
 * <p>在每次 ReAct 迭代的 Think 之前执行。
 * 从记忆系统（Memory）中召回与当前用户输入相关的历史记忆，
 * 注入到上下文供 LLM 参考，帮助 Character 做"记得之前说过什么"的连贯对话。
 *
 * <p>典型场景：用户问"接着上次的说"，MemoryInjectStep 会召回相关记忆。
 *
 * @author yijunw
 */
@Slf4j
public class MemoryInjectStep extends CharacterStep {

    private final ObjectProvider<MemoryFacade> memoryFacadeProvider;

    public MemoryInjectStep(ObjectProvider<MemoryFacade> memoryFacadeProvider) {
        super("memoryInject");
        this.memoryFacadeProvider = memoryFacadeProvider;
    }

    public MemoryInjectStep() {
        super("memoryInject");
        this.memoryFacadeProvider = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        MemoryFacade memoryFacade = memoryFacadeProvider != null ? memoryFacadeProvider.getIfAvailable() : null;
        if (memoryFacade == null) {
            return StepResult.success(getName(), "memory_disabled");
        }

        String queryText = ctx.getUserInput();
        if (queryText == null || queryText.isBlank()) {
            return StepResult.success(getName(), "no_input");
        }

        MemoryQuery query = MemoryQuery.builder()
                .text(queryText)
                .characterId(ctx.getCharacterId())
                .workspaceId(ctx.getWorkspaceId())
                .limit(5)
                .build();

        List<MemorySearchResult> results = memoryFacade.recall(query);
        ctx.setRecalledMemories(results);

        log.debug("[MemoryInjectStep] [{}] Recalled {} memories",
                ctx.getCurrentIteration(), results.size());

        return StepResult.success(getName(), results);
    }
}