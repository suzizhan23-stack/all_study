package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_word_book_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWordBookProgress {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "word_book_id", nullable = false, length = 36)
    private String wordBookId;

    @Column(name = "strategy_id", nullable = false, length = 36)
    private String strategyId;

    @Column(name = "daily_count", nullable = false)
    private int dailyCount;

    @Column(name = "current_position", nullable = false)
    private int currentPosition;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
