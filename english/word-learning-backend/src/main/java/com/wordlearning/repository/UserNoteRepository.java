package com.wordlearning.repository;

import com.wordlearning.entity.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNoteRepository extends JpaRepository<UserNote, String> {
    List<UserNote> findByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId);
}
