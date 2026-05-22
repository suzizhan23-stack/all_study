package com.wordlearning.repository;

import com.wordlearning.entity.WordBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordBookRepository extends JpaRepository<WordBook, Long> {
    Optional<WordBook> findByUuid(String uuid);
}
