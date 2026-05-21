package com.wordlearning.repository;

import com.wordlearning.entity.Definition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefinitionRepository extends JpaRepository<Definition, String> {
    List<Definition> findByWordIdOrderBySortOrder(String wordId);
}
