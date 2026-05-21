package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_relations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordRelation {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(name = "related_word_id", nullable = false, length = 36)
    private String relatedWordId;

    @Column(name = "relation_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RelationType relationType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RelationType {
        synonym, antonym, hyponym, hypernym, derivative, see_also
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
