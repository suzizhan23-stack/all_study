package com.wordlearning.service;

import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.dto.response.WrongWordResponse;
import com.wordlearning.entity.ReviewLog;
import com.wordlearning.entity.Word;
import com.wordlearning.repository.ReviewLogRepository;
import com.wordlearning.repository.WordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WrongWordService {

    private final ReviewLogRepository reviewLogRepository;
    private final WordRepository wordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public WrongWordResponse getWrongWords(String userId, String quizType, int days, int page, int size) {
        LocalDateTime since = days > 0 ? LocalDateTime.now().minusDays(days) : LocalDateTime.now().minusDays(30);

        String countJpql = "SELECT COUNT(DISTINCT rl.wordId) FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = false AND rl.reviewedAt >= :since";
        if (quizType != null && !quizType.isBlank()) {
            countJpql += " AND rl.quizType = :quizType";
        }
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class)
                .setParameter("userId", userId)
                .setParameter("since", since);
        if (quizType != null && !quizType.isBlank()) {
            countQuery.setParameter("quizType", ReviewLog.QuizType.valueOf(quizType));
        }
        long total = countQuery.getSingleResult();

        String jpql = "SELECT rl.wordId, COUNT(rl) as cnt FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = false AND rl.reviewedAt >= :since";
        if (quizType != null && !quizType.isBlank()) {
            jpql += " AND rl.quizType = :quizType";
        }
        jpql += " GROUP BY rl.wordId ORDER BY cnt DESC";

        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("userId", userId)
                .setParameter("since", since)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size);
        if (quizType != null && !quizType.isBlank()) {
            query.setParameter("quizType", ReviewLog.QuizType.valueOf(quizType));
        }
        List<Object[]> results = query.getResultList();

        List<WrongWordResponse.WordGroup> wordGroups = new ArrayList<>();
        int maxWrongCount = 0;
        String topWrongWord = null;
        int topWrongCount = 0;

        Map<String, Integer> typeCountMap = new HashMap<>();

        for (Object[] row : results) {
            String wordId = (String) row[0];
            Number cnt = (Number) row[1];
            int wrongCount = cnt.intValue();

            Word w = wordRepository.findById(wordId).orElse(null);
            if (w == null) continue;

            List<ReviewLog> logs = reviewLogRepository
                    .findByUserIdAndWordIdAndIsCorrectFalseOrderByReviewedAtDesc(userId, wordId);

            List<WrongWordResponse.LogEntry> logEntries = logs.stream()
                    .map(l -> WrongWordResponse.LogEntry.builder()
                            .id(l.getId())
                            .quizType(l.getQuizType().name())
                            .wrongAnswer(l.getWrongAnswer())
                            .reviewedAt(l.getReviewedAt() != null ? l.getReviewedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());

            for (ReviewLog log : logs) {
                typeCountMap.merge(log.getQuizType().name(), 1, Integer::sum);
            }

            String lastWrong = logs.isEmpty() ? null : logs.get(0).getReviewedAt().toString();

            if (wrongCount > topWrongCount) {
                topWrongCount = wrongCount;
                topWrongWord = w.getWord();
            }

            maxWrongCount = Math.max(maxWrongCount, wrongCount);

            wordGroups.add(WrongWordResponse.WordGroup.builder()
                    .wordId(wordId)
                    .word(w.getWord())
                    .meaningCn(w.getMeaningCn())
                    .wrongCount(wrongCount)
                    .lastWrong(lastWrong)
                    .logs(logEntries)
                    .build());
        }

        List<WrongWordResponse.WeakType> weakTypes = typeCountMap.entrySet().stream()
                .map(e -> WrongWordResponse.WeakType.builder()
                        .quizType(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        WrongWordResponse.Stats stats = WrongWordResponse.Stats.builder()
                .totalWrongWords((int) total)
                .recentDays(days > 0 ? days : 30)
                .topWrongWord(topWrongWord != null
                        ? WrongWordResponse.TopWrong.builder().word(topWrongWord).count(topWrongCount).build()
                        : null)
                .weakTypes(weakTypes)
                .build();

        return WrongWordResponse.builder()
                .stats(stats)
                .words(wordGroups)
                .pagination(new PageResponse.Pagination(page, size, total, (int) Math.ceil((double) total / size)))
                .build();
    }

    public List<String> generateReviewQueue(String userId, int limit, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        TypedQuery<Object[]> query = entityManager.createQuery(
                        "SELECT rl.wordId, COUNT(rl) as cnt FROM ReviewLog rl " +
                                "WHERE rl.userId = :userId AND rl.isCorrect = false AND rl.reviewedAt >= :since " +
                                "GROUP BY rl.wordId ORDER BY cnt DESC", Object[].class)
                .setParameter("userId", userId)
                .setParameter("since", since)
                .setMaxResults(limit);

        List<Object[]> results = query.getResultList();
        List<String> wordIds = new ArrayList<>();

        for (Object[] row : results) {
            String wordId = (String) row[0];
            Word w = wordRepository.findById(wordId).orElse(null);
            if (w != null) {
                w.setNextReview(LocalDateTime.now());
                wordRepository.save(w);
                wordIds.add(wordId);
            }
        }

        return wordIds;
    }
}
