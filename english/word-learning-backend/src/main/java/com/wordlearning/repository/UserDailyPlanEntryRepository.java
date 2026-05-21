package com.wordlearning.repository;

import com.wordlearning.entity.UserDailyPlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserDailyPlanEntryRepository extends JpaRepository<UserDailyPlanEntry, String> {
    List<UserDailyPlanEntry> findByUserIdAndPlanDateOrderBySortOrder(String userId, LocalDate date);
}
