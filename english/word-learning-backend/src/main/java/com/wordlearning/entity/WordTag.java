package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WordTagId.class)
public class WordTag {
    @Id
    @Column(name = "word_id", length = 36)
    private String wordId;

    @Id
    @Column(length = 30)
    private String tag;

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

@Data
class WordTagId implements java.io.Serializable {
    private String wordId;
    private String tag;
}
