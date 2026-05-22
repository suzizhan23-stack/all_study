package com.wordlearning.repository;

import com.wordlearning.entity.WordVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordVariantRepository extends JpaRepository<WordVariant, Long> {
    List<WordVariant> findByWordId(Long wordId);
    Optional<WordVariant> findByUuid(String uuid);
}
