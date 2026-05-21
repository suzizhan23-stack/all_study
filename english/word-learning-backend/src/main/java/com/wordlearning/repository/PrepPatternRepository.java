package com.wordlearning.repository;

import com.wordlearning.entity.PrepPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrepPatternRepository extends JpaRepository<PrepPattern, String> {
    List<PrepPattern> findByWordIdOrderByFrequencyDesc(String wordId);
}
