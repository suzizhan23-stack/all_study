package com.wordlearning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserBadgeId.class)
public class UserBadge {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Id
    @Column(name = "badge_id", length = 36)
    private String badgeId;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) earnedAt = LocalDateTime.now();
    }
}

@Data
class UserBadgeId implements java.io.Serializable {
    private String userId;
    private String badgeId;
}
