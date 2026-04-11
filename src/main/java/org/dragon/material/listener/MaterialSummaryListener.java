package org.dragon.material.listener;

import java.util.concurrent.CompletableFuture;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.material.Material;
import org.dragon.material.MaterialSummaryEvent;
import org.dragon.material.ParsedMaterialContent;
import org.dragon.workspace.material.WorkspaceMaterialService;
import org.dragon.character.builtin.BuiltInCharacterFactory;
import org.dragon.character.Character;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 物料摘要监听器
 * 异步监听物料事件并生成摘要
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialSummaryListener {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final WorkspaceMaterialService workspaceMaterialService;
    private final CharacterCaller characterCaller;
    private final ConfigApplication promptManager;

    /**
     * 监听物料上传事件，异步生成摘要
     */
    @Async
    @EventListener
    public void onMaterialUploaded(MaterialSummaryEvent event) {
        if (event.getEventType() != MaterialSummaryEvent.EventType.UPLOADED) {
            return;
        }

        Material material = event.getMaterial();
        log.info("[MaterialSummaryListener] Handling uploaded material: {}", material.getId());

        CompletableFuture.runAsync(() -> generateSummary(material));
    }

    /**
     * 生成物料摘要
     */
    private void generateSummary(Material material) {
        try {
            // 获取 MaterialSummary Character
            String workspaceId = material.getWorkspaceId();
            Character summaryChar = builtInCharacterFactory
                    .getOrCreateMaterialSummaryCharacter(workspaceId);

            // 获取原始解析内容（如果已有）
            var contentOpt = workspaceMaterialService.getParsedContent(material.getId());
            String contentToSummarize = "";
            if (contentOpt.isPresent() && contentOpt.get().getTextContent() != null) {
                contentToSummarize = contentOpt.get().getTextContent();
            } else {
                // 如果还没有解析，先解析
                ParsedMaterialContent parsed = workspaceMaterialService.parseMaterial(material.getId());
                if (parsed != null) {
                    contentToSummarize = parsed.getTextContent() != null ? parsed.getTextContent() : "";
                }
            }

            if (contentToSummarize.isEmpty()) {
                log.warn("[MaterialSummaryListener] No content to summarize for material: {}", material.getId());
                return;
            }

            // 获取摘要 prompt（使用 ConfigApplication 动态获取）
            String prompt= promptManager.getPrompt(workspaceId, summaryChar.getId(), PromptKeys.MATERIAL_SUMMARY);
      
            // 调用 Character 生成摘要
            String summarizedContent = characterCaller.call(summaryChar, prompt);

            // 将摘要存回 ParsedMaterialContent
            if (contentOpt.isPresent()) {
                workspaceMaterialService.updateParsedContentSummary(contentOpt.get().getId(), summarizedContent);
            }

            log.info("[MaterialSummaryListener] Summary generated for material: {}", material.getId());

        } catch (Exception e) {
            log.error("[MaterialSummaryListener] Failed to generate summary for material {}: {}",
                    material.getId(), e.getMessage(), e);
        }
    }
}