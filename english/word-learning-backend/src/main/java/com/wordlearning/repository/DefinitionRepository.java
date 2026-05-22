package com.wordlearning.repository;

import com.wordlearning.entity.Definition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefinitionRepository extends JpaRepository<Definition, Long> {
    List<Definition> findByWordIdOrderBySortOrder(Long wordId);
    Optional<Definition> findByUuid(String uuid);
}
