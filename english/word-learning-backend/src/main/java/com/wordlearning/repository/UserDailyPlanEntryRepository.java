package com.wordlearning.repository;

import com.wordlearning.entity.UserDailyPlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyPlanEntryRepository extends JpaRepository<UserDailyPlanEntry, Long> {
    List<UserDailyPlanEntry> findByUserIdAndPlanDateOrderBySortOrder(Long userId, LocalDate date);
    Optional<UserDailyPlanEntry> findByUuid(String uuid);
}
