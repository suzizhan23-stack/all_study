package com.wordlearning.repository;

import com.wordlearning.entity.ContentRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentRatingRepository extends JpaRepository<ContentRating, String> {
    Optional<ContentRating> findByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId);
}
