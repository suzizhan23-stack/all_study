package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Definition {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(name = "meaning_en", nullable = false, columnDefinition = "TEXT")
    private String meaningEn;

    @Column(name = "meaning_cn", nullable = false, length = 500)
    private String meaningCn;

    @Column(name = "pos_detail", length = 30)
    private String posDetail;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
