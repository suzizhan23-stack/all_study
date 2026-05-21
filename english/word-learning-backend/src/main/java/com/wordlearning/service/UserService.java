package com.wordlearning.service;

import com.wordlearning.dto.request.ProfileUpdateRequest;
import com.wordlearning.dto.response.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserStatRepository userStatRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final UserSettingRepository userSettingRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserStat stat = userStatRepository.findByUserId(userId).orElse(null);

        long totalReviews = entityManager.createQuery(
                        "SELECT COUNT(rl) FROM ReviewLog rl WHERE rl.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        long correctReviews = entityManager.createQuery(
                        "SELECT COUNT(rl) FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = true", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        double accuracy = totalReviews > 0 ? (double) correctReviews / totalReviews * 100 : 0.0;

        int xpNextLevel = calculateXpForNextLevel(stat != null ? stat.getLevel() : 1);

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .email(user.getEmail())
                .role(user.getRole().name())
                .level(stat != null ? stat.getLevel() : 1)
                .xp(stat != null ? stat.getXp() : 0)
                .xpNextLevel(xpNextLevel)
                .streakDays(stat != null ? stat.getStreakDays() : 0)
                .longestStreak(stat != null ? stat.getLongestStreak() : 0)
                .totalWordsLearned(stat != null ? stat.getTotalWordsLearned() : 0)
                .totalReviews((int) totalReviews)
                .totalTimeSpentSec(stat != null ? stat.getTotalTimeSpentSec() : 0)
                .accuracy(accuracy)
                .defaultStrategyId(user.getDefaultStrategyId())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    private int calculateXpForNextLevel(int currentLevel) {
        return currentLevel * 100;
    }

    public void updateProfile(String userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettings(String userId) {
        List<UserSetting> settings = userSettingRepository.findByUserId(userId);
        Map<String, String> result = new LinkedHashMap<>();
        for (UserSetting s : settings) {
            result.put(s.getSettingKey(), s.getSettingValue());
        }
        return result;
    }

    public void saveSettings(String userId, Map<String, String> settings) {
        if (settings == null) return;
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            UserSetting setting = userSettingRepository.findByUserIdAndSettingKey(userId, entry.getKey())
                    .orElse(null);
            if (setting != null) {
                setting.setSettingValue(entry.getValue());
                userSettingRepository.save(setting);
            } else {
                UserSetting newSetting = UserSetting.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .settingKey(entry.getKey())
                        .settingValue(entry.getValue())
                        .build();
                userSettingRepository.save(newSetting);
            }
        }
    }

    @Transactional(readOnly = true)
    public ActivityResponse getActivity(String userId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<LearningActivity> activities = entityManager.createQuery(
                        "SELECT la FROM LearningActivity la WHERE la.userId = :userId AND la.activityDate >= :since ORDER BY la.activityDate ASC", LearningActivity.class)
                .setParameter("userId", userId)
                .setParameter("since", since)
                .getResultList();

        List<ActivityResponse.ActivityItem> items = activities.stream()
                .map(a -> ActivityResponse.ActivityItem.builder()
                        .date(a.getActivityDate().toString())
                        .wordsStudied(a.getWordsStudied())
                        .reviewsDone(a.getReviewsDone())
                        .timeSpentSec(a.getTimeSpentSec())
                        .correctCount(a.getCorrectCount())
                        .wrongCount(a.getWrongCount())
                        .build())
                .collect(Collectors.toList());

        return ActivityResponse.builder().activity(items).build();
    }

    @Transactional(readOnly = true)
    public BadgeListResponse getBadges(String userId) {
        List<Badge> allBadges = badgeRepository.findAllByOrderBySortOrder();
        List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);
        Set<String> earnedBadgeIds = userBadges.stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        Map<String, LocalDateTime> earnedAtMap = userBadges.stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, UserBadge::getEarnedAt));

        List<BadgeListResponse.BadgeItem> items = allBadges.stream()
                .map(b -> BadgeListResponse.BadgeItem.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .icon(b.getIcon())
                        .description(b.getDescription())
                        .isEarned(earnedBadgeIds.contains(b.getId()))
                        .earnedAt(earnedBadgeIds.contains(b.getId()) && earnedAtMap.get(b.getId()) != null
                                ? earnedAtMap.get(b.getId()).toString() : null)
                        .build())
                .collect(Collectors.toList());

        return BadgeListResponse.builder()
                .badges(items)
                .earnedCount((int) earnedBadgeIds.size())
                .totalCount(allBadges.size())
                .build();
    }

    @Transactional(readOnly = true)
    public Integer getStreak(String userId) {
        UserStat stat = userStatRepository.findByUserId(userId).orElse(null);
        return stat != null ? stat.getStreakDays() : 0;
    }

    public void updateXP(String userId, int xpGained) {
        UserStat stat = userStatRepository.findByUserId(userId).orElse(null);
        if (stat == null) {
            stat = UserStat.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .xp(xpGained)
                    .level(1)
                    .streakDays(0)
                    .longestStreak(0)
                    .totalWordsLearned(0)
                    .totalReviews(0)
                    .totalTimeSpentSec(0)
                    .isPublic(false)
                    .build();
        } else {
            stat.setXp(stat.getXp() + xpGained);
        }

        int newLevel = calculateLevel(stat.getXp());
        if (newLevel > stat.getLevel()) {
            stat.setLevel(newLevel);
        }

        userStatRepository.save(stat);
    }

    private int calculateLevel(int xp) {
        if (xp < 100) return 1;
        return (int) (Math.sqrt(xp / 100.0)) + 1;
    }
}
