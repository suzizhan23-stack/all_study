package com.wordlearning.repository;

import com.wordlearning.entity.WordRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRelationRepository extends JpaRepository<WordRelation, Long> {
    List<WordRelation> findByWordIdOrRelatedWordId(Long wordId1, Long wordId2);
    Optional<WordRelation> findByUuid(String uuid);
}
