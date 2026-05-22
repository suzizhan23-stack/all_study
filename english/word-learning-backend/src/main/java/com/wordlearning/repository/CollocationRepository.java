package com.wordlearning.repository;

import com.wordlearning.entity.Collocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollocationRepository extends JpaRepository<Collocation, Long> {
    List<Collocation> findByWordIdOrderByFrequencyDesc(Long wordId);
    Optional<Collocation> findByUuid(String uuid);
}
