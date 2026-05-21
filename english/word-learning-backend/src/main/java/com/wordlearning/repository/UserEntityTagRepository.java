package com.wordlearning.repository;

import com.wordlearning.entity.UserEntityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityTagRepository extends JpaRepository<UserEntityTag, String> {
}
