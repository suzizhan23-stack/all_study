package com.wordlearning.service;

import com.wordlearning.dto.response.DashboardResponse;
import com.wordlearning.entity.Article;
import com.wordlearning.entity.DailyRecommendation;
import com.wordlearning.entity.ReadingProgress;
import com.wordlearning.entity.Word;
import com.wordlearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final UserStatRepository userStatRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final UserSettingRepository userSettingRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final WordRepository wordRepository;
    private final ArticleRepository articleRepository;

    private static final Map<Integer, Integer> LEVEL_XP = Map.of(
            1, 0, 2, 100, 3, 250, 4, 500, 5, 800,
            6, 1200, 7, 1700, 8, 2300, 9, 3000, 10, 4000
    );

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String userId) {
        var stat = userStatRepository.findByUserId(userId).orElse(null);

        LocalDate today = LocalDate.now();
        var activity = learningActivityRepository.findByUserIdAndActivityDate(userId, today).orElse(null);

        var settings = userSettingRepository.findByUserId(userId);
        int dailyGoal = settings.stream()
                .filter(s -> "daily_goal".equals(s.getSettingKey()))
                .findFirst()
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue()); }
                    catch (NumberFormatException e) { return 20; }
                })
                .orElse(20);

        int wordsStudied = activity != null ? activity.getWordsStudied() : 0;
        int pct = dailyGoal > 0 ? (int) ((double) wordsStudied / dailyGoal * 100) : 0;

        var recommendations = dailyRecommendationRepository
                .findByUserIdAndRecommendDateAndIsConsumedFalseOrderByReason(userId, today);

        List<String> wordIds = recommendations.stream()
                .map(DailyRecommendation::getEntityId)
                .toList();
        List<Word> words = wordRepository.findAllById(wordIds);
        Map<String, String> wordTextMap = words.stream()
                .collect(Collectors.toMap(Word::getId, Word::getWord));

        List<DashboardResponse.Recommendation> recList = recommendations.stream()
                .map(r -> DashboardResponse.Recommendation.builder()
                        .id(r.getId())
                        .entityType(r.getEntityType())
                        .entityId(r.getEntityId())
                        .word(wordTextMap.getOrDefault(r.getEntityId(), ""))
                        .reason(r.getReason())
                        .isConsumed(r.isConsumed())
                        .build())
                .toList();

        @SuppressWarnings("unchecked")
        int wrongWordCount = ((List<Object[]>) (List<?>) reviewLogRepository.countWrongWordsByUser(
                userId, PageRequest.of(0, 100))).size();

        List<Article> allArticles = articleRepository.findAll();
        Set<String> articlesWithProgress = readingProgressRepository.findAll().stream()
                .filter(rp -> rp.getUserId().equals(userId))
                .map(ReadingProgress::getArticleId)
                .collect(Collectors.toSet());
        long unreadArticleCount = allArticles.stream()
                .filter(a -> !articlesWithProgress.contains(a.getId()))
                .count();

        int level = stat != null ? stat.getLevel() : 1;
        int xp = stat != null ? stat.getXp() : 0;
        int xpNextLevel = calculateXpNextLevel(xp, level);

        LocalDateTime now = LocalDateTime.now();
        var dueWords = wordRepository.findByStageAndNextReviewLessThanEqual(0, now, PageRequest.of(0, 1));

        return DashboardResponse.builder()
                .today(DashboardResponse.TodayProgress.builder()
                        .wordsStudied(wordsStudied)
                        .dailyGoal(dailyGoal)
                        .pct(Math.min(pct, 100))
                        .build())
                .stats(DashboardResponse.Stats.builder()
                        .streakDays(stat != null ? stat.getStreakDays() : 0)
                        .longestStreak(stat != null ? stat.getLongestStreak() : 0)
                        .level(level)
                        .xp(xp)
                        .xpNextLevel(xpNextLevel)
                        .build())
                .recommendations(recList)
                .quick(DashboardResponse.QuickCounts.builder()
                        .dueReviewCount(dueWords.size())
                        .unreadArticleCount((int) unreadArticleCount)
                        .wrongWordCount(wrongWordCount)
                        .build())
                .build();
    }

    private int calculateXpNextLevel(int currentXp, int currentLevel) {
        for (int lvl = currentLevel + 1; lvl <= 10; lvl++) {
            int requirement = LEVEL_XP.getOrDefault(lvl, Integer.MAX_VALUE);
            if (currentXp < requirement) {
                return requirement;
            }
        }
        return LEVEL_XP.getOrDefault(10, 4000);
    }

    public void consumeRecommendation(String userId, String recId) {
        DailyRecommendation rec = dailyRecommendationRepository.findById(recId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found: " + recId));
        if (!rec.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        rec.setConsumed(true);
        dailyRecommendationRepository.save(rec);
    }
}
