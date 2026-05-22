package com.wordlearning.repository;

import com.wordlearning.entity.WordTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordTagRepository extends JpaRepository<WordTag, Long> {
    List<WordTag> findByWordId(Long wordId);
    Optional<WordTag> findByUuid(String uuid);
}
