package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "word_id", nullable = false, length = 36)
    private String wordId;

    @Column(name = "quiz_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QuizType quizType;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "wrong_answer", columnDefinition = "TEXT")
    private String wrongAnswer;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public enum QuizType {
        meaning, spelling, listening, usage, sentence
    }

    @PrePersist
    protected void onCreate() {
        if (reviewedAt == null) reviewedAt = LocalDateTime.now();
    }
}
