package com.wordlearning.repository;

import com.wordlearning.entity.UserWordBookProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWordBookProgressRepository extends JpaRepository<UserWordBookProgress, String> {
    Optional<UserWordBookProgress> findByUserIdAndWordBookId(String userId, String wordBookId);
}
