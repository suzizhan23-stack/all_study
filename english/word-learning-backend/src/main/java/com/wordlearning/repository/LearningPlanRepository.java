package com.wordlearning.repository;

import com.wordlearning.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {
    List<LearningPlan> findByIsActiveTrueOrderBySortOrder();
    Optional<LearningPlan> findByUuid(String uuid);
}
