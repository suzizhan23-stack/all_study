package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "words")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, length = 50)
    private String word;

    @Column(nullable = false, length = 30)
    private String pos;

    @Column(name = "first_letter", nullable = false, length = 1)
    private String firstLetter;

    @Column(name = "phonetic_uk", length = 100)
    private String phoneticUk;

    @Column(name = "phonetic_us", length = 100)
    private String phoneticUs;

    @Column(name = "audio_uk", length = 500)
    private String audioUk;

    @Column(name = "audio_us", length = 500)
    private String audioUs;

    @Column(name = "meaning_cn", length = 500)
    private String meaningCn;

    @Column(columnDefinition = "TEXT")
    private String etymology;

    @Column(name = "etymology_cn", columnDefinition = "TEXT")
    private String etymologyCn;

    @Column(length = 50)
    private String source;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private int frequency;

    @Column(nullable = false)
    private int stage;

    @Column(nullable = false)
    private int confidence;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "consecutive_correct", nullable = false)
    private int consecutiveCorrect;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "next_review")
    private LocalDateTime nextReview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
