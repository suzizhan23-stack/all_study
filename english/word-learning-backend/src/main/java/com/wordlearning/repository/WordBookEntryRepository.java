package com.wordlearning.repository;

import com.wordlearning.entity.WordBookEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordBookEntryRepository extends JpaRepository<WordBookEntry, Long> {
    List<WordBookEntry> findByWordBookIdOrderBySortOrder(Long wordBookId, Pageable p);
    Optional<WordBookEntry> findByUuid(String uuid);
}
