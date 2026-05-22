package com.wordlearning.repository;

import com.wordlearning.entity.LearningActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    Optional<LearningActivity> findByUserIdAndActivityDate(Long userId, LocalDate date);
    Optional<LearningActivity> findByUuid(String uuid);
}
