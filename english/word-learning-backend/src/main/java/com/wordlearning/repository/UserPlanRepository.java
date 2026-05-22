package com.wordlearning.repository;

import com.wordlearning.entity.UserPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {
    List<UserPlan> findByUserIdAndCompletedAtIsNull(Long userId);
    Optional<UserPlan> findByUuid(String uuid);
}
