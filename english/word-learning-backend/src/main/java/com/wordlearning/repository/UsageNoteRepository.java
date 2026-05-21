package com.wordlearning.repository;

import com.wordlearning.entity.UsageNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageNoteRepository extends JpaRepository<UsageNote, String> {
    List<UsageNote> findByWordIdOrderBySortOrder(String wordId);
}
