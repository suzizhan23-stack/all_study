package com.wordlearning.repository;

import com.wordlearning.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, String> {
    Optional<ReadingProgress> findByUserIdAndArticleId(String userId, String articleId);
}
