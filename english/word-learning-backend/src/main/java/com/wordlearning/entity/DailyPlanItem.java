package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_plan_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyPlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_book_id", nullable = false)
    private Long wordBookId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "is_key_point", nullable = false)
    @Builder.Default
    private boolean isKeyPoint = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
