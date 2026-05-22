package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "examples")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Example {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "sentence_en", nullable = false, columnDefinition = "TEXT")
    private String sentenceEn;

    @Column(name = "sentence_cn", nullable = false, columnDefinition = "TEXT")
    private String sentenceCn;

    @Column(name = "source_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "source_detail", length = 200)
    private String sourceDetail;

    @Column(name = "article_id")
    private Long articleId;

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
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
