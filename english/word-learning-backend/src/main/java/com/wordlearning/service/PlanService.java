package com.wordlearning.service;

import com.wordlearning.dto.request.GeneratePlanRequest;
import com.wordlearning.dto.request.PlanEntryRequest;
import com.wordlearning.dto.response.DailyPlanResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.dto.response.PlanDatesResponse;
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

    @Transactional(readOnly = true)
    public Map<String, Object> getActivePlan(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<UserPlan> userPlans = userPlanRepository.findByUserIdAndCompletedAtIsNull(user.getId());
        if (userPlans.isEmpty()) {
            return Collections.emptyMap();
        }
        UserPlan userPlan = userPlans.get(0);
        LearningPlan plan = learningPlanRepository.findById(userPlan.getPlanId())
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", userPlan.getUuid());
        result.put("userId", user.getUuid());
        result.put("planId", plan != null ? plan.getUuid() : null);
        result.put("currentDay", userPlan.getCurrentDay());
        result.put("startedAt", userPlan.getStartedAt() != null ? userPlan.getStartedAt().toString() : null);
        result.put("completedAt", userPlan.getCompletedAt() != null ? userPlan.getCompletedAt().toString() : null);

        if (plan != null) {
            Map<String, Object> planDetail = new LinkedHashMap<>();
            planDetail.put("id", plan.getUuid());
            planDetail.put("name", plan.getName());
            planDetail.put("description", plan.getDescription());
            planDetail.put("targetLevel", plan.getTargetLevel());
            planDetail.put("durationDays", plan.getDurationDays());
            planDetail.put("dailyWordCount", plan.getDailyWordCount());
            result.put("plan", planDetail);
        }

        return result;
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
                    entry.getSortOrder(), "manual", planDate);
            if (we != null) words.add(we);
        }

        for (DailyPlanItem item : planItems) {
            DailyPlanResponse.WordEntry we = buildWordEntry(
                    item.getUuid(), item.getWordId(), item.isCompleted(),
                    item.getSortOrder(), "auto", planDate);
            if (we != null) words.add(we);
        }

        words.sort(Comparator.comparingInt(DailyPlanResponse.WordEntry::getSortOrder));

        return DailyPlanResponse.builder()
                .date(date)
                .total(total)
                .completed(completed)
                .words(words)
                .build();
    }

    private DailyPlanResponse.WordEntry buildWordEntry(String id, Long wordId, boolean completed,
                                                        int sortOrder, String source, LocalDate planDate) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) return null;

        List<DailyPlanResponse.CollocationCompact> collocations = collocationRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .limit(3)
                .map(c -> DailyPlanResponse.CollocationCompact.builder()
                        .text(c.getCollocation())
                        .frequency(c.getFrequency())
                        .build())
                .collect(Collectors.toList());

        List<DailyPlanResponse.PrepCompact> preps = prepPatternRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .limit(3)
                .map(p -> DailyPlanResponse.PrepCompact.builder()
                        .pattern(p.getPattern())
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

    public int generateDailyPlan(String userId, GeneratePlanRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        StudyStrategy strategy = studyStrategyRepository.findByUuid(req.getStrategyId())
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found"));
        WordBook wordBook = wordBookRepository.findByUuid(req.getWordBookId())
                .orElseThrow(() -> new ResourceNotFoundException("WordBook not found"));
        LocalDate planDate = LocalDate.parse(req.getDate());
        int count = req.getCount() != null ? req.getCount() : 10;

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

        List<Word> selected = words.stream()
                .limit(count)
                .collect(Collectors.toList());

        int generated = 0;
        for (Word w : selected) {
            long exists = entityManager.createQuery(
                            "SELECT COUNT(d) FROM DailyPlanItem d WHERE d.userId = :userId AND d.planDate = :date AND d.wordId = :wordId", Long.class)
                    .setParameter("userId", user.getId())
                    .setParameter("date", planDate)
                    .setParameter("wordId", w.getId())
                    .getSingleResult();
            if (exists > 0) continue;

            DailyPlanItem item = DailyPlanItem.builder()
                    .userId(user.getId())
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
}
