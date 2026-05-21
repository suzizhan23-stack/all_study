package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_daily_plan_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyPlanEntry {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
