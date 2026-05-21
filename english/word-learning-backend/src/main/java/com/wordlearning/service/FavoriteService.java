package com.wordlearning.service;

import com.wordlearning.dto.request.FavoriteRequest;
import com.wordlearning.dto.request.FolderRequest;
import com.wordlearning.dto.response.FolderItemsResponse;
import com.wordlearning.dto.response.FolderListResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.entity.Favorite;
import com.wordlearning.entity.FavoriteFolder;
import com.wordlearning.entity.UserEntityTag;
import com.wordlearning.entity.Word;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.FavoriteFolderRepository;
import com.wordlearning.repository.FavoriteRepository;
import com.wordlearning.repository.UserEntityTagRepository;
import com.wordlearning.repository.WordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteFolderRepository favoriteFolderRepository;
    private final FavoriteRepository favoriteRepository;
    private final WordRepository wordRepository;
    private final UserEntityTagRepository userEntityTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public FolderListResponse getFolders(String userId, String category) {
        List<FavoriteFolder> folders = favoriteFolderRepository.findByUserIdOrderBySortOrder(userId);
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            folders = folders.stream()
                    .filter(f -> f.getCategory().name().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        List<FolderListResponse.FolderItem> items = folders.stream()
                .map(f -> {
                    long count = entityManager.createQuery(
                                    "SELECT COUNT(fv) FROM Favorite fv WHERE fv.folderId = :folderId", Long.class)
                            .setParameter("folderId", f.getId())
                            .getSingleResult();
                    return FolderListResponse.FolderItem.builder()
                            .id(f.getId())
                            .name(f.getName())
                            .category(f.getCategory().name())
                            .isDefault(f.isDefault())
                            .isPublic(f.isPublic())
                            .itemCount((int) count)
                            .sortOrder(f.getSortOrder())
                            .createdAt(f.getCreatedAt() != null ? f.getCreatedAt().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return FolderListResponse.builder().folders(items).build();
    }

    public FolderListResponse.FolderItem createFolder(String userId, FolderRequest req) {
        Integer maxSort = entityManager.createQuery(
                        "SELECT MAX(f.sortOrder) FROM FavoriteFolder f WHERE f.userId = :userId", Integer.class)
                .setParameter("userId", userId)
                .getSingleResult();

        FavoriteFolder folder = FavoriteFolder.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(req.getName())
                .category(FavoriteFolder.Category.valueOf(req.getCategory()))
                .isDefault(false)
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : false)
                .sortOrder(maxSort != null ? maxSort + 1 : 0)
                .build();
        favoriteFolderRepository.save(folder);

        return FolderListResponse.FolderItem.builder()
                .id(folder.getId())
                .name(folder.getName())
                .category(folder.getCategory().name())
                .isDefault(folder.isDefault())
                .isPublic(folder.isPublic())
                .itemCount(0)
                .sortOrder(folder.getSortOrder())
                .createdAt(folder.getCreatedAt() != null ? folder.getCreatedAt().toString() : null)
                .build();
    }

    public void updateFolder(String userId, String folderId, FolderRequest req) {
        FavoriteFolder folder = favoriteFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Folder not found");
        }
        folder.setName(req.getName());
        if (req.getIsPublic() != null) {
            folder.setPublic(req.getIsPublic());
        }
        favoriteFolderRepository.save(folder);
    }

    public void deleteFolder(String userId, String folderId) {
        FavoriteFolder folder = favoriteFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Folder not found");
        }
        entityManager.createQuery("DELETE FROM Favorite fv WHERE fv.folderId = :folderId")
                .setParameter("folderId", folderId)
                .executeUpdate();
        favoriteFolderRepository.delete(folder);
    }

    public void reorderFolders(String userId, List<String> order) {
        for (int i = 0; i < order.size(); i++) {
            FavoriteFolder folder = favoriteFolderRepository.findById(order.get(i))
                    .orElse(null);
            if (folder != null && folder.getUserId().equals(userId)) {
                folder.setSortOrder(i);
                favoriteFolderRepository.save(folder);
            }
        }
    }

    @Transactional(readOnly = true)
    public FolderItemsResponse getFolderItems(String userId, String folderId, int page, int size, String sort) {
        FavoriteFolder folder = favoriteFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        String countJpql = "SELECT COUNT(fv) FROM Favorite fv WHERE fv.folderId = :folderId AND fv.userId = :userId";
        long total = entityManager.createQuery(countJpql, Long.class)
                .setParameter("folderId", folderId)
                .setParameter("userId", userId)
                .getSingleResult();

        StringBuilder jpql = new StringBuilder(
                "SELECT fv FROM Favorite fv WHERE fv.folderId = :folderId AND fv.userId = :userId");
        if ("createdAt".equals(sort)) {
            jpql.append(" ORDER BY fv.createdAt DESC");
        } else {
            jpql.append(" ORDER BY fv.createdAt ASC");
        }

        TypedQuery<Favorite> query = entityManager.createQuery(jpql.toString(), Favorite.class)
                .setParameter("folderId", folderId)
                .setParameter("userId", userId)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size);

        List<Favorite> favorites = query.getResultList();

        List<FolderItemsResponse.FolderItemDetail> details = favorites.stream()
                .map(fv -> {
                    String wordText = null;
                    String meaning = null;
                    if ("word".equals(fv.getEntityType().name())) {
                        Word w = wordRepository.findById(fv.getEntityId()).orElse(null);
                        if (w != null) {
                            wordText = w.getWord();
                            meaning = w.getMeaningCn();
                        }
                    }
                    return FolderItemsResponse.FolderItemDetail.builder()
                            .id(fv.getId())
                            .entityType(fv.getEntityType().name())
                            .entityId(fv.getEntityId())
                            .word(wordText)
                            .meaningCn(meaning)
                            .note(fv.getNote())
                            .createdAt(fv.getCreatedAt() != null ? fv.getCreatedAt().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());

        FolderItemsResponse.FolderRef folderRef = FolderItemsResponse.FolderRef.builder()
                .id(folder.getId())
                .name(folder.getName())
                .category(folder.getCategory().name())
                .build();

        return FolderItemsResponse.builder()
                .folder(folderRef)
                .items(details)
                .pagination(new PageResponse.Pagination(page, size, total, (int) Math.ceil((double) total / size)))
                .build();
    }

    public void addFavorite(String userId, FavoriteRequest req) {
        Favorite favorite = Favorite.builder()
                .id(UUID.randomUUID().toString())
                .folderId(req.getFolderId())
                .entityType(Favorite.EntityType.valueOf(req.getEntityType()))
                .entityId(req.getEntityId())
                .note(req.getNote())
                .build();
        favoriteRepository.save(favorite);
    }

    public void removeFavorite(String userId, String favId) {
        Favorite fav = favoriteRepository.findById(favId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        favoriteRepository.delete(fav);
    }

    public void batchDeleteFavorites(String userId, List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        entityManager.createQuery(
                        "DELETE FROM Favorite fv WHERE fv.id IN :ids AND fv.userId = :userId")
                .setParameter("ids", ids)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public void batchTagWords(String userId, List<String> wordIds, String tagId) {
        if (wordIds == null || wordIds.isEmpty()) return;
        List<UserEntityTag> tags = new ArrayList<>();
        for (String wordId : wordIds) {
            UserEntityTag tag = UserEntityTag.builder()
                    .userId(userId)
                    .tagId(tagId)
                    .entityType("word")
                    .entityId(wordId)
                    .build();
            tags.add(tag);
        }
        userEntityTagRepository.saveAll(tags);
    }
}
