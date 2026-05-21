package com.wordlearning.repository;

import com.wordlearning.entity.WordForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordFormRepository extends JpaRepository<WordForm, String> {
    List<WordForm> findByWordId(String wordId);
}
