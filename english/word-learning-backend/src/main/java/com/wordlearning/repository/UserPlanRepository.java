package com.wordlearning.repository;

import com.wordlearning.entity.UserPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlan, String> {
    List<UserPlan> findByUserIdAndCompletedAtIsNull(String userId);
}
