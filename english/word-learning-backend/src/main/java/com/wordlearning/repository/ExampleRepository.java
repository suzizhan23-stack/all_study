package com.wordlearning.repository;

import com.wordlearning.entity.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExampleRepository extends JpaRepository<Example, Long> {
    List<Example> findByWordIdOrderByFrequencyDesc(Long wordId);
    Optional<Example> findByUuid(String uuid);
}
