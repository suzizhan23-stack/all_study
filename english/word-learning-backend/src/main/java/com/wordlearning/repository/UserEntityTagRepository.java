package com.wordlearning.repository;

import com.wordlearning.entity.UserEntityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEntityTagRepository extends JpaRepository<UserEntityTag, Long> {
    Optional<UserEntityTag> findByUuid(String uuid);
}
