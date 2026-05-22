package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

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
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (reviewedAt == null) reviewedAt = LocalDateTime.now();
    }
}
