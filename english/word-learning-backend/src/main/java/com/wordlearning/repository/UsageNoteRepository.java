package com.wordlearning.repository;

import com.wordlearning.entity.UsageNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageNoteRepository extends JpaRepository<UsageNote, Long> {
    List<UsageNote> findByWordIdOrderBySortOrder(Long wordId);
    Optional<UsageNote> findByUuid(String uuid);
}
