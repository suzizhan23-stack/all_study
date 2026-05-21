package com.wordlearning.repository;

import com.wordlearning.entity.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, String> {
    Optional<Word> findByWord(String word);
    List<Word> findByWordStartingWith(String prefix, Pageable p);
    List<Word> findByPosAndIdNot(String pos, String id, Pageable p);
    List<Word> findByStageAndNextReviewLessThanEqual(int stage, LocalDateTime time, Pageable p);
}
