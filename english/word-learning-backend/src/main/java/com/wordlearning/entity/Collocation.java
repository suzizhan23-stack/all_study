package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collocation {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(nullable = false, length = 200)
    private String collocation;

    @Column(nullable = false, length = 200)
    private String translation;

    @Column(nullable = false)
    private int frequency;

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
