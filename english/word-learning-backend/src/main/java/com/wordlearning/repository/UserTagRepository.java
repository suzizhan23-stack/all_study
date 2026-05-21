package com.wordlearning.repository;

import com.wordlearning.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, String> {
    List<UserTag> findByUserId(String userId);
}
