package com.wordlearning.repository;

import com.wordlearning.entity.DailyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, String> {
    List<DailyRecommendation> findByUserIdAndRecommendDateAndIsConsumedFalseOrderByReason(String userId, LocalDate date);
}
