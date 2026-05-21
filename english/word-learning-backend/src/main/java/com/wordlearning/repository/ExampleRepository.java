package com.wordlearning.repository;

import com.wordlearning.entity.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExampleRepository extends JpaRepository<Example, String> {
    List<Example> findByWordIdOrderByFrequencyDesc(String wordId);
}
