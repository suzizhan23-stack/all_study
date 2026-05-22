package com.wordlearning.service;

import com.wordlearning.dto.request.ReviewResultRequest;
import com.wordlearning.dto.response.ReviewQueueResponse;
import com.wordlearning.dto.response.ReviewResultResponse;
import com.wordlearning.entity.*;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final UserStatRepository userStatRepository;

    @Transactional(readOnly = true)
    public ReviewQueueResponse getQueue(String userId, String mode, int limit, String source) {
        LocalDateTime now = LocalDateTime.now();
        List<Word> dueWords = wordRepository.findByStageAndNextReviewLessThanEqual(0, now,
                PageRequest.of(0, limit));

        List<ReviewQueueResponse.ReviewItem> items = dueWords.stream()
                .map(w -> ReviewQueueResponse.ReviewItem.builder()
                        .wordId(w.getUuid())
                        .word(w.getWord())
                        .phoneticUk(w.getPhoneticUk())
                        .phoneticUs(w.getPhoneticUs())
                        .meaningCn(w.getMeaningCn())
                        .pos(w.getPos())
                        .stage(w.getStage())
                        .consecutiveCorrect(w.getConsecutiveCorrect())
                        .easeFactor(w.getEaseFactor() != null ? w.getEaseFactor().doubleValue() : 2.5)
                        .intervalDays(w.getIntervalDays())
                        .lastReviewedAt(w.getLastReviewedAt() != null ? w.getLastReviewedAt().toString() : null)
                        .nextReview(w.getNextReview() != null ? w.getNextReview().toString() : null)
                        .build())
                .toList();

        long newWordsAvailable = wordRepository.findByStageAndNextReviewLessThanEqual(0, now,
                PageRequest.of(0, Integer.MAX_VALUE)).size();

        return ReviewQueueResponse.builder()
                .queue(items)
                .total(items.size())
                .newWordsAvailable((int) newWordsAvailable)
                .build();
    }

    public ReviewResultResponse submitResult(String userId, ReviewResultRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(req.getWordId())
                .orElseThrow(() -> new ResourceNotFoundException("Word", req.getWordId()));

        ReviewLog.QuizType quizType;
        try {
            quizType = ReviewLog.QuizType.valueOf(req.getQuizType());
        } catch (IllegalArgumentException e) {
            quizType = ReviewLog.QuizType.meaning;
        }

        ReviewLog log = ReviewLog.builder()
                .userId(user.getId())
                .wordId(word.getId())
                .quizType(quizType)
                .isCorrect(req.getIsCorrect())
                .responseTimeMs(req.getResponseTimeMs())
                .wrongAnswer(req.getWrongAnswer())
                .reviewedAt(LocalDateTime.now())
                .build();
        reviewLogRepository.save(log);

        double easeFactor = word.getEaseFactor() != null ? word.getEaseFactor().doubleValue() : 2.5;
        int intervalDays = word.getIntervalDays();
        int consecutiveCorrect = word.getConsecutiveCorrect();
        int stage = word.getStage();

        if (req.getIsCorrect()) {
            consecutiveCorrect++;
            if (consecutiveCorrect == 1) {
                intervalDays = 1;
            } else if (consecutiveCorrect == 2) {
                intervalDays = 6;
            } else {
                intervalDays = (int) Math.round(intervalDays * easeFactor);
            }
            easeFactor = easeFactor + 0.1;
            if (easeFactor < 1.3) easeFactor = 1.3;
            stage = Math.min(stage + 1, 5);
        } else {
            consecutiveCorrect = 0;
            intervalDays = 1;
            easeFactor = Math.max(easeFactor - 0.2, 1.3);
            stage = Math.max(stage - 1, 0);
        }

        BigDecimal ef = BigDecimal.valueOf(easeFactor).setScale(2, RoundingMode.HALF_UP);
        word.setEaseFactor(ef);
        word.setIntervalDays(intervalDays);
        word.setConsecutiveCorrect(consecutiveCorrect);
        word.setStage(stage);
        word.setReviewCount(word.getReviewCount() + 1);
        word.setLastReviewedAt(LocalDateTime.now());
        word.setNextReview(LocalDateTime.now().plusDays(intervalDays));
        wordRepository.save(word);

        LocalDate today = LocalDate.now();
        LearningActivity activity = learningActivityRepository
                .findByUserIdAndActivityDate(user.getId(), today)
                .orElse(null);
        if (activity == null) {
            activity = LearningActivity.builder()
                    .userId(user.getId())
                    .activityDate(today)
                    .wordsStudied(word.getReviewCount() == 1 && req.getIsCorrect() ? 1 : 0)
                    .reviewsDone(1)
                    .timeSpentSec(req.getResponseTimeMs() != null ? req.getResponseTimeMs() / 1000 : 0)
                    .correctCount(req.getIsCorrect() ? 1 : 0)
                    .wrongCount(req.getIsCorrect() ? 0 : 1)
                    .build();
        } else {
            activity.setReviewsDone(activity.getReviewsDone() + 1);
            if (req.getIsCorrect()) {
                activity.setCorrectCount(activity.getCorrectCount() + 1);
                if (word.getReviewCount() == 1) {
                    activity.setWordsStudied(activity.getWordsStudied() + 1);
                }
            } else {
                activity.setWrongCount(activity.getWrongCount() + 1);
            }
            if (req.getResponseTimeMs() != null) {
                activity.setTimeSpentSec(activity.getTimeSpentSec() + req.getResponseTimeMs() / 1000);
            }
        }
        learningActivityRepository.save(activity);

        UserStat stat = userStatRepository.findByUserId(user.getId()).orElse(null);
        if (stat == null) {
            stat = UserStat.builder()
                    .userId(user.getId())
                    .xp(0)
                    .level(1)
                    .streakDays(0)
                    .longestStreak(0)
                    .totalWordsLearned(0)
                    .totalReviews(0)
                    .totalTimeSpentSec(0)
                    .isPublic(false)
                    .build();
        }

        int xpGained = 0;
        if (req.getIsCorrect()) {
            xpGained = 10;
            if (consecutiveCorrect >= 5) xpGained += 5;
            if (intervalDays >= 21) xpGained += 10;
        } else {
            xpGained = 1;
        }

        stat.setXp(stat.getXp() + xpGained);
        stat.setTotalReviews(stat.getTotalReviews() + 1);
        if (word.getReviewCount() == 1 && req.getIsCorrect()) {
            stat.setTotalWordsLearned(stat.getTotalWordsLearned() + 1);
        }
        if (req.getResponseTimeMs() != null) {
            stat.setTotalTimeSpentSec(stat.getTotalTimeSpentSec() + req.getResponseTimeMs() / 1000);
        }
        userStatRepository.save(stat);

        return ReviewResultResponse.builder()
                .xpGained(xpGained)
                .stage(stage)
                .nextReview(word.getNextReview().toString())
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getDistractors(String wordId, String pos, int count) {
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        List<Word> words = wordRepository.findByPosAndIdNot(pos, word.getId(), PageRequest.of(0, count));
        return words.stream().map(Word::getMeaningCn).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReviewStats(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        var todayActivity = learningActivityRepository.findByUserIdAndActivityDate(user.getId(), today).orElse(null);

        List<LearningActivity> weeklyActivities = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            learningActivityRepository.findByUserIdAndActivityDate(user.getId(), weekStart.plusDays(i))
                    .ifPresent(weeklyActivities::add);
        }

        int weeklyReviews = weeklyActivities.stream().mapToInt(LearningActivity::getReviewsDone).sum();
        int weeklyCorrect = weeklyActivities.stream().mapToInt(LearningActivity::getCorrectCount).sum();
        int weeklyWrong = weeklyActivities.stream().mapToInt(LearningActivity::getWrongCount).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayReviews", todayActivity != null ? todayActivity.getReviewsDone() : 0);
        stats.put("todayCorrect", todayActivity != null ? todayActivity.getCorrectCount() : 0);
        stats.put("todayWrong", todayActivity != null ? todayActivity.getWrongCount() : 0);
        stats.put("weeklyReviews", weeklyReviews);
        stats.put("weeklyCorrect", weeklyCorrect);
        stats.put("weeklyWrong", weeklyWrong);

        return stats;
    }
}
