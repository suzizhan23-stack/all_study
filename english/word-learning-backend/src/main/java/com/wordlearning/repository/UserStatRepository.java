package com.wordlearning.repository;

import com.wordlearning.entity.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatRepository extends JpaRepository<UserStat, String> {
    Optional<UserStat> findByUserId(String userId);
}
