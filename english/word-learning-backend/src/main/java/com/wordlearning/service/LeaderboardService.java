package com.wordlearning.service;

import com.wordlearning.dto.response.BadgeListResponse;
import com.wordlearning.dto.response.LeaderboardResponse;
import com.wordlearning.entity.*;
import com.wordlearning.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaderboardService {

    private final UserStatRepository userStatRepository;
    private final UserRepository userRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(String type, int limit, String currentUserId) {
        List<LeaderboardResponse.LeaderEntry> entries;
        String resolvedType = (type != null) ? type : "global";

        switch (resolvedType) {
            case "weekly":
                entries = getWeeklyLeaderboard(limit);
                break;
            case "streak":
                entries = getStreakLeaderboard(limit);
                break;
            default:
                entries = getGlobalLeaderboard(limit);
                break;
        }

        Integer myRank = null;
        if (currentUserId != null) {
            for (LeaderboardResponse.LeaderEntry e : entries) {
                if (e.getUserId().equals(currentUserId)) {
                    myRank = e.getRank();
                    break;
                }
            }
            if (myRank == null) {
                myRank = getCurrentUserRank(resolvedType, currentUserId);
            }
        }

        return LeaderboardResponse.builder()
                .type(resolvedType)
                .myRank(myRank)
                .leaderboard(entries)
                .build();
    }

    private List<LeaderboardResponse.LeaderEntry> getGlobalLeaderboard(int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                        "SELECT s, u FROM UserStat s JOIN User u ON s.userId = u.id ORDER BY s.xp DESC", Object[].class)
                .setMaxResults(limit);
        List<Object[]> results = query.getResultList();

        List<LeaderboardResponse.LeaderEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            UserStat stat = (UserStat) row[0];
            User user = (User) row[1];
            entries.add(buildEntry(rank++, user, stat));
        }
        return entries;
    }

    private List<LeaderboardResponse.LeaderEntry> getWeeklyLeaderboard(int limit) {
        LocalDate weekAgo = LocalDate.now().minusDays(7);

        TypedQuery<Object[]> query = entityManager.createQuery(
                        "SELECT la.userId, SUM(la.wordsStudied) as totalWords, SUM(la.reviewsDone) as totalReviews, " +
                                "SUM(la.correctCount) as totalCorrect, SUM(la.wrongCount) as totalWrong " +
                                "FROM LearningActivity la WHERE la.activityDate >= :since " +
                                "GROUP BY la.userId ORDER BY totalWords DESC", Object[].class)
                .setParameter("since", weekAgo)
                .setMaxResults(limit);

        List<Object[]> results = query.getResultList();
        List<LeaderboardResponse.LeaderEntry> entries = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            String uid = (String) row[0];
            User user = userRepository.findById(uid).orElse(null);
            if (user == null) continue;

            int totalWords = ((Number) row[1]).intValue();
            long totalCorrect = ((Number) row[3]).longValue();
            long totalWrong = ((Number) row[4]).longValue();
            double accuracy = (totalCorrect + totalWrong) > 0
                    ? (double) totalCorrect / (totalCorrect + totalWrong) * 100 : 0.0;

            UserStat stat = userStatRepository.findByUserId(uid).orElse(null);

            entries.add(LeaderboardResponse.LeaderEntry.builder()
                    .rank(rank++)
                    .userId(uid)
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .xp(totalWords)
                    .level(stat != null ? stat.getLevel() : 1)
                    .streakDays(stat != null ? stat.getStreakDays() : 0)
                    .accuracy(accuracy)
                    .build());
        }
        return entries;
    }

    private List<LeaderboardResponse.LeaderEntry> getStreakLeaderboard(int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                        "SELECT s, u FROM UserStat s JOIN User u ON s.userId = u.id ORDER BY s.streakDays DESC", Object[].class)
                .setMaxResults(limit);
        List<Object[]> results = query.getResultList();

        List<LeaderboardResponse.LeaderEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            UserStat stat = (UserStat) row[0];
            User user = (User) row[1];
            entries.add(buildEntry(rank++, user, stat));
        }
        return entries;
    }

    private LeaderboardResponse.LeaderEntry buildEntry(int rank, User user, UserStat stat) {
        long totalCorrect = entityManager.createQuery(
                        "SELECT COUNT(rl) FROM ReviewLog rl WHERE rl.userId = :userId AND rl.isCorrect = true", Long.class)
                .setParameter("userId", user.getId())
                .getSingleResult();
        long totalReviews = entityManager.createQuery(
                        "SELECT COUNT(rl) FROM ReviewLog rl WHERE rl.userId = :userId", Long.class)
                .setParameter("userId", user.getId())
                .getSingleResult();
        double accuracy = totalReviews > 0 ? (double) totalCorrect / totalReviews * 100 : 0.0;

        return LeaderboardResponse.LeaderEntry.builder()
                .rank(rank)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .xp(stat != null ? stat.getXp() : 0)
                .level(stat != null ? stat.getLevel() : 1)
                .streakDays(stat != null ? stat.getStreakDays() : 0)
                .accuracy(accuracy)
                .build();
    }

    private int getCurrentUserRank(String type, String userId) {
        String jpql;
        switch (type) {
            case "weekly":
                LocalDate weekAgo = LocalDate.now().minusDays(7);
                TypedQuery<Long> weeklyQuery = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT la.userId) FROM LearningActivity la " +
                                "WHERE la.activityDate >= :since AND la.userId IN (" +
                                "SELECT la2.userId FROM LearningActivity la2 WHERE la2.activityDate >= :since " +
                                "GROUP BY la2.userId HAVING SUM(la2.wordsStudied) > (" +
                                "SELECT COALESCE(SUM(la3.wordsStudied), 0) FROM LearningActivity la3 " +
                                "WHERE la3.userId = :userId AND la3.activityDate >= :since))", Long.class);
                weeklyQuery.setParameter("since", weekAgo);
                weeklyQuery.setParameter("userId", userId);
                return weeklyQuery.getSingleResult().intValue() + 1;
            case "streak":
                UserStat streakStat = userStatRepository.findByUserId(userId).orElse(null);
                int userStreak = streakStat != null ? streakStat.getStreakDays() : 0;
                Long streakRank = entityManager.createQuery(
                                "SELECT COUNT(s) FROM UserStat s WHERE s.streakDays > :streak", Long.class)
                        .setParameter("streak", userStreak)
                        .getSingleResult();
                return streakRank.intValue() + 1;
            default:
                UserStat globalStat = userStatRepository.findByUserId(userId).orElse(null);
                int userXp = globalStat != null ? globalStat.getXp() : 0;
                Long globalRank = entityManager.createQuery(
                                "SELECT COUNT(s) FROM UserStat s WHERE s.xp > :xp", Long.class)
                        .setParameter("xp", userXp)
                        .getSingleResult();
                return globalRank.intValue() + 1;
        }
    }

    @Transactional(readOnly = true)
    public List<Badge> getAllBadges() {
        return badgeRepository.findAllByOrderBySortOrder();
    }
}
