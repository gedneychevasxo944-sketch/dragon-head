package org.dragon.workspace.material;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 物料事件发布器
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布物料上传事件
     */
    public void publishUploaded(Material material) {
        publish(material, MaterialSummaryEvent.EventType.UPLOADED);
    }

    /**
     * 发布物料解析完成事件
     */
    public void publishParsed(Material material) {
        publish(material, MaterialSummaryEvent.EventType.PARSED);
    }

    private void publish(Material material, MaterialSummaryEvent.EventType eventType) {
        MaterialSummaryEvent event = new MaterialSummaryEvent(this, material, eventType);
        log.info("[MaterialEventPublisher] Publishing event: type={}, materialId={}",
                eventType, material.getId());
        eventPublisher.publishEvent(event);
    }
}