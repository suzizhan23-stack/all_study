package com.wordlearning.repository;

import com.wordlearning.entity.FavoriteFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteFolderRepository extends JpaRepository<FavoriteFolder, Long> {
    List<FavoriteFolder> findByUserIdOrderBySortOrder(Long userId);
    Optional<FavoriteFolder> findByUuid(String uuid);
}
