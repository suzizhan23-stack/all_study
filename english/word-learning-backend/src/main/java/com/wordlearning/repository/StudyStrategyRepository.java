package com.wordlearning.repository;

import com.wordlearning.entity.StudyStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyStrategyRepository extends JpaRepository<StudyStrategy, Long> {
    Optional<StudyStrategy> findByUuid(String uuid);
}
