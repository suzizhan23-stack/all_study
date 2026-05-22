package com.wordlearning.repository;

import com.wordlearning.entity.DailyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, Long> {
    List<DailyRecommendation> findByUserIdAndRecommendDateAndIsConsumedFalseOrderByReason(Long userId, LocalDate date);
    Optional<DailyRecommendation> findByUuid(String uuid);
}
