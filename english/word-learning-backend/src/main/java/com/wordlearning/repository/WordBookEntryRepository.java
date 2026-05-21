package com.wordlearning.repository;

import com.wordlearning.entity.WordBookEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordBookEntryRepository extends JpaRepository<WordBookEntry, String> {
    List<WordBookEntry> findByWordBookIdOrderBySortOrder(String wordBookId, Pageable p);
}
