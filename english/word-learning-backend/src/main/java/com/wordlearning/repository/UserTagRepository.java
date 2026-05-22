package com.wordlearning.repository;

import com.wordlearning.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    List<UserTag> findByUserId(Long userId);
    Optional<UserTag> findByUuid(String uuid);
}
