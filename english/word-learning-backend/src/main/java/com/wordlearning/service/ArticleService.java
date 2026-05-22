package com.wordlearning.service;

import com.wordlearning.dto.response.ArticleDetailResponse;
import com.wordlearning.dto.response.ArticleListResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.entity.Article;
import com.wordlearning.entity.ReadingProgress;
import com.wordlearning.entity.UserStat;
import com.wordlearning.entity.Word;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final WordRepository wordRepository;
    private final UserStatRepository userStatRepository;

    @Transactional(readOnly = true)
    public ArticleListResponse getArticles(String userId, Integer difficulty, String source, String status,
                                           int page, int size) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<Article> allArticles = articleRepository.findAll();

        if (difficulty != null) {
            allArticles = allArticles.stream()
                    .filter(a -> a.getDifficulty() == difficulty)
                    .toList();
        }
        if (source != null && !source.isEmpty()) {
            allArticles = allArticles.stream()
                    .filter(a -> source.equals(a.getSourceName()))
                    .toList();
        }

        List<Article> filtered = new ArrayList<>();
        for (Article a : allArticles) {
            var progress = readingProgressRepository.findByUserIdAndArticleId(user.getId(), a.getId()).orElse(null);
            if (status != null && !status.isEmpty()) {
                switch (status) {
                    case "unread" -> {
                        if (progress != null) continue;
                    }
                    case "in_progress" -> {
                        if (progress == null || progress.isCompleted()) continue;
                    }
                    case "completed" -> {
                        if (progress == null || !progress.isCompleted()) continue;
                    }
                }
            }
            filtered.add(a);
        }

        int total = filtered.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Article> pageArticles = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        List<ArticleListResponse.ArticleItem> items = pageArticles.stream()
                .map(a -> {
                    var progress = readingProgressRepository.findByUserIdAndArticleId(user.getId(), a.getId()).orElse(null);
                    ArticleListResponse.ProgressInfo progressInfo = null;
                    if (progress != null) {
                        progressInfo = ArticleListResponse.ProgressInfo.builder()
                                .scrollPosition(progress.getScrollPosition())
                                .isCompleted(progress.isCompleted())
                                .wordsLookedUp(progress.getWordsLookedUp())
                                .lastReadAt(progress.getLastReadAt() != null
                                        ? progress.getLastReadAt().toString() : null)
                                .build();
                    }
                    return ArticleListResponse.ArticleItem.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .author(a.getAuthor())
                            .sourceName(a.getSourceName())
                            .difficulty(a.getDifficulty())
                            .wordCount(a.getWordCount())
                            .coverImage(null)
                            .publishedAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                            .tags(new ArrayList<>())
                            .progress(progressInfo)
                            .build();
                })
                .toList();

        return ArticleListResponse.builder()
                .list(items)
                .pagination(new PageResponse.Pagination(page, size, total,
                        (int) Math.ceil((double) total / size)))
                .build();
    }

    @Transactional(readOnly = true)
    public ArticleDetailResponse getArticleDetail(String userId, String articleId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Article article = articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));

        var progress = readingProgressRepository.findByUserIdAndArticleId(user.getId(), article.getId()).orElse(null);
        ArticleListResponse.ProgressInfo progressInfo = null;
        if (progress != null) {
            progressInfo = ArticleListResponse.ProgressInfo.builder()
                    .scrollPosition(progress.getScrollPosition())
                    .isCompleted(progress.isCompleted())
                    .wordsLookedUp(progress.getWordsLookedUp())
                    .lastReadAt(progress.getLastReadAt() != null ? progress.getLastReadAt().toString() : null)
                    .build();
        }

        return ArticleDetailResponse.builder()
                .id(article.getUuid())
                .title(article.getTitle())
                .content(article.getContent())
                .contentType(null)
                .author(article.getAuthor())
                .sourceName(article.getSourceName())
                .sourceUrl(article.getSourceUrl())
                .difficulty(article.getDifficulty())
                .wordCount(article.getWordCount())
                .publishedAt(article.getCreatedAt() != null ? article.getCreatedAt().toString() : null)
                .progress(progressInfo)
                .vocabulary(new ArrayList<>())
                .build();
    }

    public void saveProgress(String userId, String articleId, int scrollPos, Integer readingTimeSec) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Article article = articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
        ReadingProgress progress = readingProgressRepository
                .findByUserIdAndArticleId(user.getId(), article.getId())
                .orElse(null);
        if (progress == null) {
            progress = ReadingProgress.builder()
                    .userId(user.getId())
                    .articleId(article.getId())
                    .scrollPosition(scrollPos)
                    .isCompleted(false)
                    .wordsLookedUp(0)
                    .lastReadAt(LocalDateTime.now())
                    .build();
        } else {
            progress.setScrollPosition(scrollPos);
            progress.setLastReadAt(LocalDateTime.now());
        }
        readingProgressRepository.save(progress);
    }

    public void markComplete(String userId, String articleId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Article article = articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
        ReadingProgress progress = readingProgressRepository
                .findByUserIdAndArticleId(user.getId(), article.getId())
                .orElse(null);
        if (progress == null) {
            progress = ReadingProgress.builder()
                    .userId(user.getId())
                    .articleId(article.getId())
                    .scrollPosition(0)
                    .isCompleted(true)
                    .wordsLookedUp(0)
                    .lastReadAt(LocalDateTime.now())
                    .build();
        } else {
            progress.setCompleted(true);
            progress.setLastReadAt(LocalDateTime.now());
        }
        readingProgressRepository.save(progress);

        UserStat stat = userStatRepository.findByUserId(user.getId()).orElse(null);
        if (stat != null) {
            stat.setXp(stat.getXp() + 50);
            userStatRepository.save(stat);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lookupWord(String userId, String articleId, String wordText) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Article article = articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
        Word word = wordRepository.findByWord(wordText).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        if (word != null) {
            result.put("id", word.getUuid());
            result.put("word", word.getWord());
            result.put("meaningCn", word.getMeaningCn());
            result.put("pos", word.getPos());
            result.put("phoneticUk", word.getPhoneticUk());
            result.put("phoneticUs", word.getPhoneticUs());
        } else {
            result.put("word", wordText);
            result.put("meaningCn", null);
        }

        ReadingProgress progress = readingProgressRepository
                .findByUserIdAndArticleId(user.getId(), article.getId())
                .orElse(null);
        if (progress != null) {
            progress.setWordsLookedUp(progress.getWordsLookedUp() + 1);
            readingProgressRepository.save(progress);
        }

        return result;
    }
}
