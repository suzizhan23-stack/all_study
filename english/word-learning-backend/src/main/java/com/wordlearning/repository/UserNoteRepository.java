package com.wordlearning.repository;

import com.wordlearning.entity.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNoteRepository extends JpaRepository<UserNote, Long> {
    List<UserNote> findByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    Optional<UserNote> findByUuid(String uuid);
}
