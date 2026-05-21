package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "examples")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Example {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(name = "sentence_en", nullable = false, columnDefinition = "TEXT")
    private String sentenceEn;

    @Column(name = "sentence_cn", nullable = false, columnDefinition = "TEXT")
    private String sentenceCn;

    @Column(name = "source_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "source_detail", length = 200)
    private String sourceDetail;

    @Column(name = "article_id", length = 36)
    private String articleId;

    @Column(nullable = false)
    private int frequency;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SourceType {
        CET46, KAOYAN, TOEFL, IELTS, ACADEMIC, COMMON, ARTICLE
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
