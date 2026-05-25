package com.wordlearning.service;

import com.wordlearning.dto.response.AdminOverviewResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.entity.*;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final ArticleRepository articleRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final ContentRatingRepository contentRatingRepository;
    private final WordBookEntryRepository wordBookEntryRepository;
    private final WordBookRepository wordBookRepository;
    private final CollocationRepository collocationRepository;
    private final PrepPatternRepository prepPatternRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        long totalUsers = userRepository.count();
        long totalWords = wordRepository.count();
        long totalArticles = articleRepository.count();

        long totalReviews = entityManager.createQuery("SELECT COUNT(rl) FROM ReviewLog rl", Long.class)
                .getSingleResult();

        LocalDate today = LocalDate.now();
        long activeToday = entityManager.createQuery(
                        "SELECT COUNT(la) FROM LearningActivity la WHERE la.activityDate = :today", Long.class)
                .setParameter("today", today)
                .getSingleResult();

        long newUsersToday = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.createdAt >= :start", Long.class)
                .setParameter("start", today.atStartOfDay())
                .getSingleResult();

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .activeToday((int) activeToday)
                .totalWords(totalWords)
                .totalReviews(totalReviews)
                .newUsersToday((int) newUsersToday)
                .totalArticles(totalArticles)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> getUsers(String keyword, String role, Boolean isActive,
                                                       int page, int size) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (u.username LIKE :keyword OR u.nickname LIKE :keyword OR u.email LIKE :keyword)");
            params.put("keyword", "%" + keyword + "%");
        }
        if (role != null && !role.isBlank()) {
            jpql.append(" AND u.role = :role");
            params.put("role", User.Role.valueOf(role));
        }
        if (isActive != null) {
            jpql.append(" AND u.isActive = :isActive");
            params.put("isActive", isActive);
        }
        jpql.append(" ORDER BY u.createdAt DESC");

        TypedQuery<Long> countQuery = entityManager.createQuery(
                jpql.toString().replace("SELECT u", "SELECT COUNT(u)"), Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        TypedQuery<User> query = entityManager.createQuery(jpql.toString(), User.class);
        params.forEach(query::setParameter);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        List<User> users = query.getResultList();

        List<Map<String, Object>> list = users.stream()
                .map(u -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", u.getUuid());
                    map.put("username", u.getUsername());
                    map.put("nickname", u.getNickname());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole().name());
                    map.put("isActive", u.isActive());
                    map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
                    return map;
                })
                .toList();

        return PageResponse.of(list, page, size, total);
    }

    public void toggleUserStatus(String userId, boolean active) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(active);
        userRepository.save(user);
    }

    public Word createWord(Word word) {
        if (word.getWord() != null && !word.getWord().isEmpty()) {
            word.setFirstLetter(String.valueOf(word.getWord().charAt(0)).toUpperCase());
        }
        wordRepository.save(word);
        return word;
    }

    public Word updateWord(String id, Word word) {
        Word existing = wordRepository.findByUuid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Word not found"));
        if (word.getWord() != null) {
            existing.setWord(word.getWord());
            existing.setFirstLetter(String.valueOf(word.getWord().charAt(0)).toUpperCase());
        }
        if (word.getPos() != null) existing.setPos(word.getPos());
        if (word.getPhoneticUk() != null) existing.setPhoneticUk(word.getPhoneticUk());
        if (word.getPhoneticUs() != null) existing.setPhoneticUs(word.getPhoneticUs());
        if (word.getAudioUk() != null) existing.setAudioUk(word.getAudioUk());
        if (word.getAudioUs() != null) existing.setAudioUs(word.getAudioUs());
        if (word.getMeaningCn() != null) existing.setMeaningCn(word.getMeaningCn());
        if (word.getEtymology() != null) existing.setEtymology(word.getEtymology());
        if (word.getEtymologyCn() != null) existing.setEtymologyCn(word.getEtymologyCn());
        if (word.getSource() != null) existing.setSource(word.getSource());
        existing.setDifficulty(word.getDifficulty());
        existing.setFrequency(word.getFrequency());
        wordRepository.save(existing);
        return existing;
    }

    public void deleteWord(String id) {
        Word word = wordRepository.findByUuid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Word not found"));
        Long wid = word.getId();

        entityManager.createQuery("DELETE FROM ReviewLog rl WHERE rl.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM Favorite fv WHERE fv.entityType = 'word' AND fv.entityId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM WordBookEntry e WHERE e.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM Collocation c WHERE c.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM PrepPattern p WHERE p.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM DailyPlanItem d WHERE d.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM UserDailyPlanEntry e WHERE e.wordId = :wordId")
                .setParameter("wordId", wid)
                .executeUpdate();

        wordRepository.delete(word);
    }

    public Collocation updateCollocation(String uuid, String collocation, String translation, Integer frequency) {
        Collocation c = collocationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Collocation not found"));
        if (collocation != null) c.setCollocation(collocation);
        if (translation != null) c.setTranslation(translation);
        if (frequency != null) c.setFrequency(frequency);
        return collocationRepository.save(c);
    }

    public void deleteCollocation(String uuid) {
        Collocation c = collocationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Collocation not found"));
        collocationRepository.delete(c);
    }

    public PrepPattern updatePrepPattern(String uuid, String pattern, String translation, String preposition, Integer frequency) {
        PrepPattern p = prepPatternRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("PrepPattern not found"));
        if (pattern != null) p.setPattern(pattern);
        if (translation != null) p.setTranslation(translation);
        if (preposition != null) p.setPreposition(preposition);
        if (frequency != null) p.setFrequency(frequency);
        return prepPatternRepository.save(p);
    }

    public void deletePrepPattern(String uuid) {
        PrepPattern p = prepPatternRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("PrepPattern not found"));
        prepPatternRepository.delete(p);
    }

    public void batchImportWords(List<Map<String, Object>> words, String wordBookId) {
        if (words == null || words.isEmpty()) return;
        WordBook wordBook = wordBookRepository.findByUuid(wordBookId)
                .orElseThrow(() -> new ResourceNotFoundException("WordBook", wordBookId));
        int sortOrder = 0;

        for (Map<String, Object> raw : words) {
            String wordText = (String) raw.get("word");
            if (wordText == null || wordText.isBlank()) continue;

            Word existingWord = wordRepository.findByWord(wordText).orElse(null);
            if (existingWord == null) {
                existingWord = Word.builder()
                        .word(wordText)
                        .pos(raw.getOrDefault("pos", "other").toString())
                        .firstLetter(String.valueOf(wordText.charAt(0)).toUpperCase())
                        .meaningCn(raw.getOrDefault("meaningCn", "").toString())
                        .phoneticUk(raw.getOrDefault("phoneticUk", "").toString())
                        .phoneticUs(raw.getOrDefault("phoneticUs", "").toString())
                        .difficulty(raw.containsKey("difficulty") ? ((Number) raw.get("difficulty")).intValue() : 1)
                        .frequency(0)
                        .stage(0)
                        .confidence(0)
                        .reviewCount(0)
                        .consecutiveCorrect(0)
                        .easeFactor(java.math.BigDecimal.valueOf(2.5))
                        .intervalDays(0)
                        .build();
                wordRepository.save(existingWord);
            }

            long exists = entityManager.createQuery(
                            "SELECT COUNT(e) FROM WordBookEntry e WHERE e.wordBookId = :bookId AND e.wordId = :wId", Long.class)
                    .setParameter("bookId", wordBook.getId())
                    .setParameter("wId", existingWord.getId())
                    .getSingleResult();
            if (exists == 0) {
                WordBookEntry entry = WordBookEntry.builder()
                        .wordBookId(wordBook.getId())
                        .wordId(existingWord.getId())
                        .sortOrder(sortOrder++)
                        .build();
                entityManager.persist(entry);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> getFeedback(int page, int size) {
        String countJpql = "SELECT COUNT(cr) FROM ContentRating cr WHERE cr.feedback IS NOT NULL AND cr.feedback <> ''";
        long total = entityManager.createQuery(countJpql, Long.class).getSingleResult();

        TypedQuery<ContentRating> query = entityManager.createQuery(
                        "SELECT cr FROM ContentRating cr WHERE cr.feedback IS NOT NULL AND cr.feedback <> '' ORDER BY cr.createdAt DESC", ContentRating.class)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size);
        List<ContentRating> ratings = query.getResultList();

        List<Map<String, Object>> list = ratings.stream()
                .map(cr -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", cr.getId());
                    map.put("userId", cr.getUserId());
                    map.put("entityType", cr.getEntityType());
                    map.put("entityId", cr.getEntityId());
                    map.put("rating", cr.getRating());
                    map.put("feedback", cr.getFeedback());
                    map.put("createdAt", cr.getCreatedAt() != null ? cr.getCreatedAt().toString() : null);
                    return map;
                })
                .toList();

        return PageResponse.of(list, page, size, total);
    }
}
