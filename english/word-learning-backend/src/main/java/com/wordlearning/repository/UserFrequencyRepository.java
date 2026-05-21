package com.wordlearning.repository;

import com.wordlearning.entity.UserFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFrequencyRepository extends JpaRepository<UserFrequency, String> {
    Optional<UserFrequency> findByUserIdAndEntityTypeAndEntityId(String userId, UserFrequency.EntityType type, String entityId);
}
