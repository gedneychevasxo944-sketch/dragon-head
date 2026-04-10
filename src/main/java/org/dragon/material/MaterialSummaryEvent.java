package org.dragon.material;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 物料摘要生成事件
 * 在物料上传后触发，异步生成摘要
 *
 * @author wyj
 * @version 1.0
 */
@Getter
public class MaterialSummaryEvent extends ApplicationEvent {

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 物料上传
         */
        UPLOADED,
        /**
         * 物料解析完成
         */
        PARSED
    }

    private final Material material;
    private final EventType eventType;

    public MaterialSummaryEvent(Object source, Material material, EventType eventType) {
        super(source);
        this.material = material;
        this.eventType = eventType;
    }
}