package com.wordlearning.repository;

import com.wordlearning.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, String> {
    List<LearningPlan> findByIsActiveTrueOrderBySortOrder();
}
