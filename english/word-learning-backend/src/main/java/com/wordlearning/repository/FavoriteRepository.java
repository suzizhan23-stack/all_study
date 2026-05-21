package com.wordlearning.repository;

import com.wordlearning.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, String> {
    List<Favorite> findByFolderIdAndUserId(String folderId, String userId);
    Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(String userId, Favorite.EntityType type, String entityId);
}
