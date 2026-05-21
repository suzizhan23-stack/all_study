package com.wordlearning.repository;

import com.wordlearning.entity.WordRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRelationRepository extends JpaRepository<WordRelation, String> {
    List<WordRelation> findByWordIdOrRelatedWordId(String wordId1, String wordId2);
}
