package com.wordlearning.repository;

import com.wordlearning.entity.WordTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordTagRepository extends JpaRepository<WordTag, String> {
    List<WordTag> findByWordId(String wordId);
}
