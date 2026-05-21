package com.wordlearning.repository;

import com.wordlearning.entity.Collocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollocationRepository extends JpaRepository<Collocation, String> {
    List<Collocation> findByWordIdOrderByFrequencyDesc(String wordId);
}
