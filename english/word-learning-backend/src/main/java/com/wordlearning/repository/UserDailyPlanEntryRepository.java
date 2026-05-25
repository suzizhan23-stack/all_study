package com.wordlearning.repository;

import com.wordlearning.entity.UserDailyPlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyPlanEntryRepository extends JpaRepository<UserDailyPlanEntry, Long> {
    List<UserDailyPlanEntry> findByUserIdAndPlanDateOrderBySortOrder(Long userId, LocalDate date);
    List<UserDailyPlanEntry> findByUserId(Long userId);
    Optional<UserDailyPlanEntry> findByUuid(String uuid);
    Optional<UserDailyPlanEntry> findByUserIdAndPlanDateAndWordId(Long userId, LocalDate planDate, Long wordId);
    void deleteByUserIdAndPlanDate(Long userId, LocalDate planDate);
}
