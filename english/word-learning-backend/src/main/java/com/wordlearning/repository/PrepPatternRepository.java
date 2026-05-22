package com.wordlearning.repository;

import com.wordlearning.entity.PrepPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrepPatternRepository extends JpaRepository<PrepPattern, Long> {
    List<PrepPattern> findByWordIdOrderByFrequencyDesc(Long wordId);
    Optional<PrepPattern> findByUuid(String uuid);
}
