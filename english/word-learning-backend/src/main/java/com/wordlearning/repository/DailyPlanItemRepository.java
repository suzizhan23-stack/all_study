package com.wordlearning.repository;

import com.wordlearning.entity.DailyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPlanItemRepository extends JpaRepository<DailyPlanItem, Long> {
    List<DailyPlanItem> findByUserIdAndPlanDateOrderBySortOrder(Long userId, LocalDate date);
    List<DailyPlanItem> findByUserId(Long userId);
    Optional<DailyPlanItem> findByUuid(String uuid);
}
