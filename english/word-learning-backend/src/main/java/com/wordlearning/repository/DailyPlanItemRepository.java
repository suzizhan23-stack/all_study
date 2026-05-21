package com.wordlearning.repository;

import com.wordlearning.entity.DailyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyPlanItemRepository extends JpaRepository<DailyPlanItem, String> {
    List<DailyPlanItem> findByUserIdAndPlanDateOrderBySortOrder(String userId, LocalDate date);
}
