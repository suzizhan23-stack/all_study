package com.wordlearning.repository;

import com.wordlearning.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByFolderIdAndUserId(Long folderId, Long userId);
    Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(Long userId, Favorite.EntityType type, Long entityId);
    Optional<Favorite> findByUuid(String uuid);
}
