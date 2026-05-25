package com.wordlearning.service;

import com.wordlearning.dto.request.GeneratePlanRequest;
import com.wordlearning.dto.request.BatchPlanEntryRequest;
import com.wordlearning.dto.request.PlanEntryRequest;
import com.wordlearning.dto.response.DailyPlanResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.dto.response.PlanDatesResponse;
import com.wordlearning.dto.response.PlanResponse;
import com.wordlearning.entity.*;
import com.wordlearning.exception.BusinessException;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {

    private final UserRepository userRepository;
    private final UserPlanRepository userPlanRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final UserDailyPlanEntryRepository userDailyPlanEntryRepository;
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final WordBookEntryRepository wordBookEntryRepository;
    private final WordRepository wordRepository;
    private final WordBookRepository wordBookRepository;
    private final StudyStrategyRepository studyStrategyRepository;
    private final CollocationRepository collocationRepository;
    private final PrepPatternRepository prepPatternRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PlanResponse createPlan(String userId, String wordBookId, String strategyId, int dailyCount) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        WordBook wordBook = wordBookRepository.findByUuid(wordBookId)
                .orElseThrow(() -> new ResourceNotFoundException("WordBook not found"));
        StudyStrategy strategy = studyStrategyRepository.findByUuid(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found"));

        List<UserPlan> activePlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (!activePlans.isEmpty()) {
            throw BusinessException.conflict("User already has an active plan");
        }

        int totalWordsInBook = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM word_book_entries WHERE word_book_id = ?1")
                .setParameter(1, wordBook.getId())
                .getSingleResult()).intValue();
        if (totalWordsInBook == 0) {
            throw BusinessException.badRequest("Word book is empty");
        }

        UserPlan userPlan = UserPlan.builder()
                .userId(user.getId())
                .wordBookId(wordBook.getId())
                .strategyId(strategy.getId())
                .startedAt(LocalDateTime.now())
                .currentDay(1)
                .dailyCount(dailyCount)
                .build();
        userPlanRepository.save(userPlan);

        generateDailyWords(user.getId(), wordBook, strategy, dailyCount, LocalDate.now(), 0);

        return buildPlanResponse(userPlan, wordBook, strategy, dailyCount);
    }

    @Transactional(readOnly = true)
    public PlanResponse getActivePlan(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<UserPlan> userPlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (userPlans.isEmpty()) {
            return null;
        }
        UserPlan userPlan = userPlans.get(0);

        int dailyCount = userPlan.getDailyCount() != null ? userPlan.getDailyCount() : 10;

        if (userPlan.getWordBookId() != null) {
            WordBook wordBook = wordBookRepository.findById(userPlan.getWordBookId()).orElse(null);
            StudyStrategy strategy = userPlan.getStrategyId() != null
                    ? studyStrategyRepository.findById(userPlan.getStrategyId()).orElse(null)
                    : null;

            int totalWordsInBook = 0;
            if (wordBook != null) {
                totalWordsInBook = ((Number) entityManager.createNativeQuery(
                                "SELECT COUNT(*) FROM word_book_entries WHERE word_book_id = ?1")
                        .setParameter(1, wordBook.getId())
                        .getSingleResult()).intValue();
            }

            LocalDate today = LocalDate.now();
            int todayWords = countTodayWords(user.getId(), today);
            int todayCompleted = countTodayCompleted(user.getId(), today);
            int totalDays = dailyCount > 0 ? (int) Math.ceil((double) totalWordsInBook / dailyCount) : 0;

            return PlanResponse.builder()
                    .id(userPlan.getUuid())
                    .type("wordbook")
                    .wordBook(wordBook != null ? PlanResponse.WordBookInfo.builder()
                            .id(wordBook.getUuid())
                            .name(wordBook.getName())
                            .wordCount(totalWordsInBook)
                            .difficultyLevel(wordBook.getDifficultyLevel())
                            .build() : null)
                    .strategy(strategy != null ? PlanResponse.StrategyInfo.builder()
                            .id(strategy.getUuid())
                            .name(strategy.getName())
                            .description(strategy.getDescription())
                            .build() : null)
                    .dailyCount(dailyCount)
                    .currentDay(userPlan.getCurrentDay())
                    .totalDays(totalDays)
                    .pct(totalDays > 0 ? (double) (userPlan.getCurrentDay() - 1) / totalDays * 100 : 0)
                    .todayWords(todayWords)
                    .todayCompleted(todayCompleted)
                    .totalWords(totalWordsInBook)
                    .startedAt(userPlan.getStartedAt() != null ? userPlan.getStartedAt().toString() : null)
                    .completed(false)
                    .build();
        }

        LearningPlan plan = userPlan.getPlanId() != null
                ? learningPlanRepository.findById(userPlan.getPlanId()).orElse(null)
                : null;

        return PlanResponse.builder()
                .id(userPlan.getUuid())
                .type("template")
                .dailyCount(dailyCount)
                .currentDay(userPlan.getCurrentDay())
                .totalDays(plan != null ? plan.getDurationDays() : 0)
                .startedAt(userPlan.getStartedAt() != null ? userPlan.getStartedAt().toString() : null)
                .completed(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LearningPlan> getPlanTemplates() {
        return learningPlanRepository.findByIsActiveTrueOrderBySortOrder();
    }

    public void joinPlan(String userId, String planId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<UserPlan> activePlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (!activePlans.isEmpty()) {
            throw BusinessException.conflict("User already has an active plan");
        }

        LearningPlan plan = learningPlanRepository.findByUuid(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        UserPlan userPlan = UserPlan.builder()
                .userId(user.getId())
                .planId(plan.getId())
                .startedAt(LocalDateTime.now())
                .currentDay(1)
                .dailyCount(plan.getDailyWordCount())
                .build();
        userPlanRepository.save(userPlan);
    }

    @Transactional(readOnly = true)
    public DailyPlanResponse getDailyWords(String userId, String date) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        LocalDate planDate = LocalDate.parse(date);

        List<UserDailyPlanEntry> userEntries = userDailyPlanEntryRepository
                .findByUserIdAndPlanDateOrderBySortOrder(user.getId(), planDate);
        List<DailyPlanItem> planItems = dailyPlanItemRepository
                .findByUserIdAndPlanDateOrderBySortOrder(user.getId(), planDate);

        int total = userEntries.size() + planItems.size();
        int completed = (int) (userEntries.stream().filter(UserDailyPlanEntry::isCompleted).count()
                + planItems.stream().filter(DailyPlanItem::isCompleted).count());

        List<DailyPlanResponse.WordEntry> words = new ArrayList<>();

        for (UserDailyPlanEntry entry : userEntries) {
            DailyPlanResponse.WordEntry we = buildWordEntry(
                    entry.getUuid(), entry.getWordId(), entry.isCompleted(),
                    entry.isKeyPoint(), entry.getSortOrder(), "manual", planDate);
            if (we != null) words.add(we);
        }

        for (DailyPlanItem item : planItems) {
            DailyPlanResponse.WordEntry we = buildWordEntry(
                    item.getUuid(), item.getWordId(), item.isCompleted(),
                    item.isKeyPoint(), item.getSortOrder(), "auto", planDate);
            if (we != null) words.add(we);
        }

        words.sort(Comparator.comparingInt(DailyPlanResponse.WordEntry::getSortOrder));

        DailyPlanResponse.DailyPlanResponseBuilder builder = DailyPlanResponse.builder()
                .date(date)
                .total(total)
                .completed(completed)
                .words(words);

        List<UserPlan> activePlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (!activePlans.isEmpty() && activePlans.get(0).getWordBookId() != null) {
            wordBookRepository.findById(activePlans.get(0).getWordBookId()).ifPresent(wb ->
                    builder.wordBook(new DailyPlanResponse.WordBookRef(wb.getUuid(), wb.getName())));
        }

        return builder.build();
    }

    private DailyPlanResponse.WordEntry buildWordEntry(String id, Long wordId, boolean completed,
                                                        boolean keyPoint, int sortOrder, String source, LocalDate planDate) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) return null;

        List<DailyPlanResponse.CollocationCompact> collocations = collocationRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .map(c -> DailyPlanResponse.CollocationCompact.builder()
                        .text(c.getCollocation())
                        .translation(c.getTranslation())
                        .frequency(c.getFrequency())
                        .build())
                .collect(Collectors.toList());

        List<DailyPlanResponse.PrepCompact> preps = prepPatternRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .map(p -> DailyPlanResponse.PrepCompact.builder()
                        .pattern(p.getPattern())
                        .translation(p.getTranslation())
                        .preposition(p.getPreposition())
                        .build())
                .collect(Collectors.toList());

        return DailyPlanResponse.WordEntry.builder()
                .id(id)
                .wordId(word.getUuid())
                .word(word.getWord())
                .phoneticUk(word.getPhoneticUk())
                .pos(word.getPos())
                .meaningCn(word.getMeaningCn())
                .isCompleted(completed)
                .isKeyPoint(keyPoint)
                .entrySource(source)
                .sortOrder(sortOrder)
                .collocations(collocations)
                .preps(preps)
                .build();
    }

    @Transactional(readOnly = true)
    public PlanDatesResponse getPlanDates(String userId, int limit) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        String sql = "SELECT DISTINCT plan_date FROM (" +
                "SELECT plan_date FROM user_daily_plan_entries WHERE user_id = ?1 " +
                "UNION " +
                "SELECT plan_date FROM daily_plan_items WHERE user_id = ?1" +
                ") d ORDER BY plan_date DESC";

        @SuppressWarnings("unchecked")
        List<Object> dateResults = entityManager.createNativeQuery(sql)
                .setParameter(1, user.getId())
                .setMaxResults(limit)
                .getResultList();

        List<PlanDatesResponse.DateItem> dateItems = dateResults.stream()
                .map(d -> {
                    LocalDate ld;
                    if (d instanceof java.sql.Date) {
                        ld = ((java.sql.Date) d).toLocalDate();
                    } else {
                        ld = LocalDate.parse(d.toString());
                    }
                    long udCount = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date", Long.class)
                            .setParameter("userId", user.getId())
                            .setParameter("date", ld)
                            .getSingleResult();
                    long dpCount = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.planDate = :date", Long.class)
                            .setParameter("userId", user.getId())
                            .setParameter("date", ld)
                            .getSingleResult();
                    long udCompleted = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date AND e.isCompleted = true", Long.class)
                            .setParameter("userId", user.getId())
                            .setParameter("date", ld)
                            .getSingleResult();
                    long dpCompleted = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.planDate = :date AND e.isCompleted = true", Long.class)
                            .setParameter("userId", user.getId())
                            .setParameter("date", ld)
                            .getSingleResult();

                    return PlanDatesResponse.DateItem.builder()
                            .date(ld.toString())
                            .count((int) (udCount + dpCount))
                            .completed((int) (udCompleted + dpCompleted))
                            .build();
                })
                .collect(Collectors.toList());

        return PlanDatesResponse.builder().dates(dateItems).build();
    }

    public void addPlanEntry(String userId, PlanEntryRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(req.getWordId())
                .orElseThrow(() -> new ResourceNotFoundException("Word", req.getWordId()));
        LocalDate planDate = LocalDate.parse(req.getPlanDate());

        long existing = entityManager.createQuery(
                        "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date AND e.wordId = :wordId", Long.class)
                .setParameter("userId", user.getId())
                .setParameter("date", planDate)
                .setParameter("wordId", word.getId())
                .getSingleResult();
        if (existing > 0) {
            throw BusinessException.conflict("Word already exists in daily plan");
        }

        Integer maxSort = entityManager.createQuery(
                        "SELECT MAX(e.sortOrder) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date", Integer.class)
                .setParameter("userId", user.getId())
                .setParameter("date", planDate)
                .getSingleResult();

        UserDailyPlanEntry entry = UserDailyPlanEntry.builder()
                .userId(user.getId())
                .planDate(planDate)
                .wordId(word.getId())
                .sortOrder(maxSort != null ? maxSort + 1 : 0)
                .isCompleted(false)
                .build();
        userDailyPlanEntryRepository.save(entry);
    }

    @Transactional
    public void addBatchPlanEntries(String userId, BatchPlanEntryRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        LocalDate planDate = LocalDate.parse(req.getPlanDate());

        Integer maxSort = entityManager.createQuery(
                        "SELECT MAX(e.sortOrder) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date", Integer.class)
                .setParameter("userId", user.getId())
                .setParameter("date", planDate)
                .getSingleResult();
        int nextSort = maxSort != null ? maxSort + 1 : 0;

        for (String wordUuid : req.getWordIds()) {
            Word word = wordRepository.findByUuid(wordUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Word", wordUuid));

            long existing = entityManager.createQuery(
                            "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date AND e.wordId = :wordId", Long.class)
                    .setParameter("userId", user.getId())
                    .setParameter("date", planDate)
                    .setParameter("wordId", word.getId())
                    .getSingleResult();
            if (existing > 0) continue;

            UserDailyPlanEntry entry = UserDailyPlanEntry.builder()
                    .userId(user.getId())
                    .planDate(planDate)
                    .wordId(word.getId())
                    .sortOrder(nextSort++)
                    .isCompleted(false)
                    .build();
            userDailyPlanEntryRepository.save(entry);
        }
    }

    public void removePlanEntry(String userId, String entryId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserDailyPlanEntry entry = userDailyPlanEntryRepository.findByUuid(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan entry not found"));
        if (!entry.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Plan entry not found");
        }
        userDailyPlanEntryRepository.delete(entry);
    }

    public void markEntryComplete(String userId, String entryId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserDailyPlanEntry entry = userDailyPlanEntryRepository.findByUuid(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan entry not found"));
        if (!entry.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Plan entry not found");
        }
        entry.setCompleted(true);
        entry.setCompletedAt(LocalDateTime.now());
        userDailyPlanEntryRepository.save(entry);
    }

    public void toggleKeyPointByWordId(String userId, String wordUuid) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordUuid));
        LocalDate today = LocalDate.now();
        var planEntry = userDailyPlanEntryRepository
                .findByUserIdAndPlanDateAndWordId(user.getId(), today, word.getId());
        if (planEntry.isPresent()) {
            UserDailyPlanEntry e = planEntry.get();
            e.setKeyPoint(!e.isKeyPoint());
            userDailyPlanEntryRepository.save(e);
            return;
        }
        List<DailyPlanItem> items = dailyPlanItemRepository
                .findByUserIdAndPlanDateOrderBySortOrder(user.getId(), today).stream()
                .filter(i -> i.getWordId().equals(word.getId()))
                .collect(Collectors.toList());
        if (!items.isEmpty()) {
            DailyPlanItem item = items.get(0);
            item.setKeyPoint(!item.isKeyPoint());
            dailyPlanItemRepository.save(item);
            return;
        }
        throw new ResourceNotFoundException("Plan entry not found");
    }

    public void toggleKeyPoint(String userId, String entryId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        var planEntry = userDailyPlanEntryRepository.findByUuid(entryId);
        if (planEntry.isPresent()) {
            UserDailyPlanEntry e = planEntry.get();
            if (!e.getUserId().equals(user.getId())) {
                throw new ResourceNotFoundException("Plan entry not found");
            }
            e.setKeyPoint(!e.isKeyPoint());
            userDailyPlanEntryRepository.save(e);
            return;
        }
        var planItem = dailyPlanItemRepository.findByUuid(entryId);
        if (planItem.isPresent()) {
            DailyPlanItem item = planItem.get();
            if (!item.getUserId().equals(user.getId())) {
                throw new ResourceNotFoundException("Plan entry not found");
            }
            item.setKeyPoint(!item.isKeyPoint());
            dailyPlanItemRepository.save(item);
            return;
        }
        throw new ResourceNotFoundException("Plan entry not found");
    }

    public int generateDailyPlan(String userId, GeneratePlanRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        StudyStrategy strategy = studyStrategyRepository.findByUuid(req.getStrategyId())
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found"));
        WordBook wordBook = wordBookRepository.findByUuid(req.getWordBookId())
                .orElseThrow(() -> new ResourceNotFoundException("WordBook not found"));
        LocalDate planDate = req.getDate() != null ? LocalDate.parse(req.getDate()) : LocalDate.now();
        int count = req.getCount() != null ? req.getCount() : 10;

        return generateDailyWords(user.getId(), wordBook, strategy, count, planDate, 0);
    }

    public PlanResponse advanceDay(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<UserPlan> activePlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (activePlans.isEmpty()) {
            throw BusinessException.badRequest("No active plan");
        }
        UserPlan userPlan = activePlans.get(0);
        if (userPlan.getWordBookId() == null) {
            throw BusinessException.badRequest("Plan is not bound to a word book");
        }

        WordBook wordBook = wordBookRepository.findById(userPlan.getWordBookId())
                .orElseThrow(() -> new ResourceNotFoundException("WordBook not found"));
        StudyStrategy strategy = userPlan.getStrategyId() != null
                ? studyStrategyRepository.findById(userPlan.getStrategyId()).orElse(null)
                : null;
        if (strategy == null) {
            throw BusinessException.badRequest("Plan has no strategy");
        }

        int dailyCount = userPlan.getDailyCount() != null ? userPlan.getDailyCount() : 10;
        int totalWords = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM word_book_entries WHERE word_book_id = ?1")
                .setParameter(1, wordBook.getId())
                .getSingleResult()).intValue();

        int wordsGeneratedSoFar = (userPlan.getCurrentDay() - 1) * dailyCount;

        if (wordsGeneratedSoFar >= totalWords) {
            userPlan.setCompletedAt(LocalDateTime.now());
            userPlanRepository.save(userPlan);
            throw BusinessException.badRequest("Word book completed");
        }

        int startOffset = wordsGeneratedSoFar;
        int newDay = userPlan.getCurrentDay() + 1;
        int wordsLeft = totalWords - wordsGeneratedSoFar;
        int todayCount = Math.min(dailyCount, wordsLeft);

        generateDailyWords(user.getId(), wordBook, strategy, todayCount, LocalDate.now(), startOffset);

        userPlan.setCurrentDay(newDay);
        userPlanRepository.save(userPlan);

        return buildPlanResponse(userPlan, wordBook, strategy, dailyCount);
    }

    private int generateDailyWords(Long userId, WordBook wordBook, StudyStrategy strategy,
                                    int count, LocalDate planDate, int offset) {
        List<WordBookEntry> entries = wordBookEntryRepository
                .findByWordBookIdOrderBySortOrder(wordBook.getId(), Pageable.unpaged());
        if (entries.isEmpty()) return 0;

        List<Word> words = new ArrayList<>();
        for (WordBookEntry entry : entries) {
            wordRepository.findById(entry.getWordId()).ifPresent(words::add);
        }

        switch (strategy.getType()) {
            case random:
                Collections.shuffle(words);
                break;
            case alphabetical:
                words.sort(Comparator.comparing(Word::getWord));
                break;
            case difficulty_asc:
                words.sort(Comparator.comparingInt(Word::getDifficulty));
                break;
            case difficulty_desc:
                words.sort(Comparator.comparingInt(Word::getDifficulty).reversed());
                break;
            default:
                break;
        }

        int end = Math.min(offset + count, words.size());
        List<Word> selected = words.subList(offset, end);

        int generated = 0;
        for (Word w : selected) {
            long exists = entityManager.createQuery(
                            "SELECT COUNT(d) FROM DailyPlanItem d WHERE d.userId = :userId AND d.planDate = :date AND d.wordId = :wordId", Long.class)
                    .setParameter("userId", userId)
                    .setParameter("date", planDate)
                    .setParameter("wordId", w.getId())
                    .getSingleResult();
            if (exists > 0) continue;

            DailyPlanItem item = DailyPlanItem.builder()
                    .userId(userId)
                    .wordBookId(wordBook.getId())
                    .planDate(planDate)
                    .wordId(w.getId())
                    .sortOrder(generated)
                    .isCompleted(false)
                    .build();
            dailyPlanItemRepository.save(item);
            generated++;
        }

        return generated;
    }

    private PlanResponse buildPlanResponse(UserPlan userPlan, WordBook wordBook, StudyStrategy strategy, int dailyCount) {
        int totalWordsInBook = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM word_book_entries WHERE word_book_id = ?1")
                .setParameter(1, wordBook.getId())
                .getSingleResult()).intValue();

        LocalDate today = LocalDate.now();
        int todayWords = countTodayWords(userPlan.getUserId(), today);
        int todayCompleted = countTodayCompleted(userPlan.getUserId(), today);
        int totalDays = dailyCount > 0 ? (int) Math.ceil((double) totalWordsInBook / dailyCount) : 0;

        return PlanResponse.builder()
                .id(userPlan.getUuid())
                .type("wordbook")
                .wordBook(PlanResponse.WordBookInfo.builder()
                        .id(wordBook.getUuid())
                        .name(wordBook.getName())
                        .wordCount(totalWordsInBook)
                        .difficultyLevel(wordBook.getDifficultyLevel())
                        .build())
                .strategy(PlanResponse.StrategyInfo.builder()
                        .id(strategy.getUuid())
                        .name(strategy.getName())
                        .description(strategy.getDescription())
                        .build())
                .dailyCount(dailyCount)
                .currentDay(userPlan.getCurrentDay())
                .totalDays(totalDays)
                .pct(totalDays > 0 ? (double) (userPlan.getCurrentDay() - 1) / totalDays * 100 : 0)
                .todayWords(todayWords)
                .todayCompleted(todayCompleted)
                .totalWords(totalWordsInBook)
                .startedAt(userPlan.getStartedAt() != null ? userPlan.getStartedAt().toString() : null)
                .completed(false)
                .build();
    }

    public PlanResponse setCurrentWordBook(String userId, String wordBookId, String strategyId, int dailyCount) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        WordBook wordBook = wordBookRepository.findByUuid(wordBookId)
                .orElseThrow(() -> new ResourceNotFoundException("WordBook not found"));
        StudyStrategy strategy = studyStrategyRepository.findByUuid(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found"));

        int totalWordsInBook = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM word_book_entries WHERE word_book_id = ?1")
                .setParameter(1, wordBook.getId())
                .getSingleResult()).intValue();
        if (totalWordsInBook == 0) {
            throw BusinessException.badRequest("Word book is empty");
        }

        List<UserPlan> activePlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        UserPlan userPlan;
        if (activePlans.isEmpty()) {
            userPlan = UserPlan.builder()
                    .userId(user.getId())
                    .wordBookId(wordBook.getId())
                    .strategyId(strategy.getId())
                    .startedAt(LocalDateTime.now())
                    .currentDay(1)
                    .dailyCount(dailyCount)
                    .build();
            userPlanRepository.save(userPlan);
        } else {
            userPlan = activePlans.get(0);
            userPlan.setWordBookId(wordBook.getId());
            userPlan.setStrategyId(strategy.getId());
            userPlan.setDailyCount(dailyCount);
            userPlanRepository.save(userPlan);
        }

        dailyPlanItemRepository.deleteByUserIdAndPlanDate(user.getId(), LocalDate.now());
        userDailyPlanEntryRepository.deleteByUserIdAndPlanDate(user.getId(), LocalDate.now());

        generateDailyWords(user.getId(), wordBook, strategy, dailyCount, LocalDate.now(), 0);

        return buildPlanResponse(userPlan, wordBook, strategy, dailyCount);
    }

    private int countTodayWords(Long userId, LocalDate date) {
        long ud = entityManager.createQuery(
                        "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date", Long.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .getSingleResult();
        long dp = entityManager.createQuery(
                        "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.planDate = :date", Long.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .getSingleResult();
        return (int) (ud + dp);
    }

    private int countTodayCompleted(Long userId, LocalDate date) {
        long ud = entityManager.createQuery(
                        "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.planDate = :date AND e.isCompleted = true", Long.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .getSingleResult();
        long dp = entityManager.createQuery(
                        "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.planDate = :date AND e.isCompleted = true", Long.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .getSingleResult();
        return (int) (ud + dp);
    }
}
