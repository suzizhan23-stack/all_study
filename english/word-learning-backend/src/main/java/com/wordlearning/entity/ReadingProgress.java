package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingProgress {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "article_id", nullable = false, length = 36)
    private String articleId;

    @Column(name = "scroll_position", nullable = false)
    private int scrollPosition;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "words_looked_up", nullable = false)
    private int wordsLookedUp;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastReadAt == null) lastReadAt = LocalDateTime.now();
    }
}
