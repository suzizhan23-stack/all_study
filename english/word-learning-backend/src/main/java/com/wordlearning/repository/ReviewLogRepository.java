package com.wordlearning.repository;

import com.wordlearning.entity.ReviewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, String> {
    List<ReviewLog> findByUserIdAndWordIdAndIsCorrectFalseOrderByReviewedAtDesc(String userId, String wordId);

    @Query("SELECT rl.wordId, COUNT(rl) as cnt FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = false GROUP BY rl.wordId ORDER BY cnt DESC")
    List<Object[]> countWrongWordsByUser(@Param("userId") String userId, Pageable p);
}
