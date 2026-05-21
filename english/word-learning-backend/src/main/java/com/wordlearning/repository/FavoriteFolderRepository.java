package com.wordlearning.repository;

import com.wordlearning.entity.FavoriteFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteFolderRepository extends JpaRepository<FavoriteFolder, String> {
    List<FavoriteFolder> findByUserIdOrderBySortOrder(String userId);
}
