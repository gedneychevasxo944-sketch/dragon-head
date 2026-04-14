package org.dragon.impression.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.impression.enums.ImpressionSentiment;
import org.dragon.impression.enums.ImpressionType;

import java.time.LocalDateTime;

/**
 * ImpressionEntity 印象实体
 * 用于存储 Character 或 Workspace 对其他 Character 或 Workspace 的评价/印象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "impression",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_impression",
                           columnNames = {"source_type", "source_id", "target_type", "target_id", "name"})
       })
public class ImpressionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private ImpressionType sourceType;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private ImpressionType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "value", length = 512)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 16)
    private ImpressionSentiment sentiment;

    @Column(name = "trust_level")
    private int trustLevel;

    @Column(name = "summary", length = 512)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (sentiment == null) {
            sentiment = ImpressionSentiment.NEUTRAL;
        }
        if (trustLevel == 0) {
            trustLevel = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
