package com.wordlearning.repository;

import com.wordlearning.entity.WordVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordVariantRepository extends JpaRepository<WordVariant, String> {
    List<WordVariant> findByWordId(String wordId);
}
