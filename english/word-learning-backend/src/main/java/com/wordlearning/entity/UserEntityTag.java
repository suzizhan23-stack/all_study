package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_entity_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserEntityTagId.class)
public class UserEntityTag {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Id
    @Column(name = "tag_id", length = 36)
    private String tagId;

    @Id
    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Id
    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

@Data
class UserEntityTagId implements java.io.Serializable {
    private String userId;
    private String tagId;
    private String entityType;
    private String entityId;
}
