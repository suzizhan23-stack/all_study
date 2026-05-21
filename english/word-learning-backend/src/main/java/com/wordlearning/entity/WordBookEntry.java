package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_book_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WordBookEntryId.class)
public class WordBookEntry {
    @Id
    @Column(name = "word_book_id", length = 36)
    private String wordBookId;

    @Id
    @Column(name = "word_id", length = 36)
    private String wordId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

@Data
class WordBookEntryId implements java.io.Serializable {
    private String wordBookId;
    private String wordId;
}
