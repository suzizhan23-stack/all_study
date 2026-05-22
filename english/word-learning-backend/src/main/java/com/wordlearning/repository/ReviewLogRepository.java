package com.wordlearning.repository;

import com.wordlearning.entity.ReviewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {
    List<ReviewLog> findByUserIdAndWordIdAndIsCorrectFalseOrderByReviewedAtDesc(Long userId, Long wordId);

    @Query("SELECT rl.wordId, COUNT(rl) as cnt FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = false GROUP BY rl.wordId ORDER BY cnt DESC")
    List<Object[]> countWrongWordsByUser(@Param("userId") Long userId, Pageable p);

    Optional<ReviewLog> findByUuid(String uuid);
}
