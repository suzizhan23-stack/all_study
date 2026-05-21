package com.wordlearning.service;

import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.dto.response.SuggestResponse;
import com.wordlearning.dto.response.WordSearchResponse;
import com.wordlearning.entity.SearchHistory;
import com.wordlearning.entity.Word;
import com.wordlearning.repository.SearchHistoryRepository;
import com.wordlearning.repository.WordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchService {

    private final WordRepository wordRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public SuggestResponse suggest(String query, int limit) {
        List<Word> words = wordRepository.findByWordStartingWith(query, PageRequest.of(0, limit));
        List<String> suggestions = words.stream().map(Word::getWord).toList();
        return SuggestResponse.builder().suggestions(suggestions).build();
    }

    @Transactional(readOnly = true)
    public WordSearchResponse search(String userId, String query, String source, String pos, int page, int size) {
        StringBuilder jpql = new StringBuilder(
                "SELECT w, CASE WHEN f.id IS NOT NULL THEN true ELSE false END FROM Word w ");
        jpql.append("LEFT JOIN Favorite f ON f.entityId = w.id AND f.entityType = 'word' ");
        jpql.append("AND f.userId = :userId WHERE w.word LIKE :query ");

        if (source != null && !source.isEmpty()) {
            jpql.append("AND w.source = :source ");
        }
        if (pos != null && !pos.isEmpty()) {
            jpql.append("AND w.pos = :pos ");
        }

        var queryObj = entityManager.createQuery(jpql.toString(), Object[].class);
        queryObj.setParameter("userId", userId);
        queryObj.setParameter("query", query + "%");
        if (source != null && !source.isEmpty()) {
            queryObj.setParameter("source", source);
        }
        if (pos != null && !pos.isEmpty()) {
            queryObj.setParameter("pos", pos);
        }

        int total = queryObj.getResultList().size();
        queryObj.setFirstResult(page * size);
        queryObj.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> results = queryObj.getResultList();

        List<WordSearchResponse.SearchResult> list = results.stream()
                .map(row -> {
                    Word w = (Word) row[0];
                    boolean isCollected = (Boolean) row[1];
                    return WordSearchResponse.SearchResult.builder()
                            .id(w.getId())
                            .word(w.getWord())
                            .phoneticUk(w.getPhoneticUk())
                            .phoneticUs(w.getPhoneticUs())
                            .pos(w.getPos())
                            .meaningCn(w.getMeaningCn())
                            .source(w.getSource())
                            .difficulty(w.getDifficulty())
                            .frequency(w.getFrequency())
                            .isCollected(isCollected)
                            .build();
                })
                .toList();

        return WordSearchResponse.builder()
                .list(list)
                .pagination(new PageResponse.Pagination(page, size, total, (int) Math.ceil((double) total / size)))
                .build();
    }

    public void saveHistory(String userId, String query, int resultCount) {
        SearchHistory history = SearchHistory.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .query(query)
                .resultCount(resultCount)
                .searchedAt(LocalDateTime.now())
                .build();
        searchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<SearchHistory> getHistory(String userId, int limit) {
        return searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    public void clearHistory(String userId) {
        List<SearchHistory> history = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
        searchHistoryRepository.deleteAll(history);
    }
}
