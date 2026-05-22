package com.wordlearning.service;

import com.wordlearning.dto.request.FavoriteRequest;
import com.wordlearning.dto.request.FolderRequest;
import com.wordlearning.dto.response.FolderItemsResponse;
import com.wordlearning.dto.response.FolderListResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.entity.*;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final UserRepository userRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final FavoriteRepository favoriteRepository;
    private final WordRepository wordRepository;
    private final UserTagRepository userTagRepository;
    private final UserEntityTagRepository userEntityTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public FolderListResponse getFolders(String userId, String category) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<FavoriteFolder> folders = favoriteFolderRepository.findByUserIdOrderBySortOrder(user.getId());
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
                            .id(f.getUuid())
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
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Integer maxSort = entityManager.createQuery(
                        "SELECT MAX(f.sortOrder) FROM FavoriteFolder f WHERE f.userId = :userId", Integer.class)
                .setParameter("userId", user.getId())
                .getSingleResult();

        FavoriteFolder folder = FavoriteFolder.builder()
                .userId(user.getId())
                .name(req.getName())
                .category(FavoriteFolder.Category.valueOf(req.getCategory()))
                .isDefault(false)
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : false)
                .sortOrder(maxSort != null ? maxSort + 1 : 0)
                .build();
        favoriteFolderRepository.save(folder);

        return FolderListResponse.FolderItem.builder()
                .id(folder.getUuid())
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
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        FavoriteFolder folder = favoriteFolderRepository.findByUuid(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Folder not found");
        }
        folder.setName(req.getName());
        if (req.getIsPublic() != null) {
            folder.setPublic(req.getIsPublic());
        }
        favoriteFolderRepository.save(folder);
    }

    public void deleteFolder(String userId, String folderId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        FavoriteFolder folder = favoriteFolderRepository.findByUuid(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Folder not found");
        }
        entityManager.createQuery("DELETE FROM Favorite fv WHERE fv.folderId = :folderId")
                .setParameter("folderId", folder.getId())
                .executeUpdate();
        favoriteFolderRepository.delete(folder);
    }

    public void reorderFolders(String userId, List<String> order) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        for (String folderUuid : order) {
            FavoriteFolder folder = favoriteFolderRepository.findByUuid(folderUuid)
                    .orElse(null);
            if (folder != null && folder.getUserId().equals(user.getId())) {
                folder.setSortOrder(order.indexOf(folderUuid));
                favoriteFolderRepository.save(folder);
            }
        }
    }

    @Transactional(readOnly = true)
    public FolderItemsResponse getFolderItems(String userId, String folderId, int page, int size, String sort) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        FavoriteFolder folder = favoriteFolderRepository.findByUuid(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        String countJpql = "SELECT COUNT(fv) FROM Favorite fv WHERE fv.folderId = :folderId AND fv.userId = :userId";
        long total = entityManager.createQuery(countJpql, Long.class)
                .setParameter("folderId", folder.getId())
                .setParameter("userId", user.getId())
                .getSingleResult();

        StringBuilder jpql = new StringBuilder(
                "SELECT fv FROM Favorite fv WHERE fv.folderId = :folderId AND fv.userId = :userId");
        if ("createdAt".equals(sort)) {
            jpql.append(" ORDER BY fv.createdAt DESC");
        } else {
            jpql.append(" ORDER BY fv.createdAt ASC");
        }

        TypedQuery<Favorite> query = entityManager.createQuery(jpql.toString(), Favorite.class)
                .setParameter("folderId", folder.getId())
                .setParameter("userId", user.getId())
                .setFirstResult((page - 1) * size)
                .setMaxResults(size);

        List<Favorite> favorites = query.getResultList();

        List<FolderItemsResponse.FolderItemDetail> details = favorites.stream()
                .map(fv -> {
                    String wordText = null;
                    String meaning = null;
                    String entityUuid = null;
                    if ("word".equals(fv.getEntityType().name())) {
                        Word w = wordRepository.findById(fv.getEntityId()).orElse(null);
                        if (w != null) {
                            wordText = w.getWord();
                            meaning = w.getMeaningCn();
                            entityUuid = w.getUuid();
                        }
                    }
                    return FolderItemsResponse.FolderItemDetail.builder()
                            .id(fv.getUuid())
                            .entityType(fv.getEntityType().name())
                            .entityId(entityUuid)
                            .word(wordText)
                            .meaningCn(meaning)
                            .note(fv.getNote())
                            .createdAt(fv.getCreatedAt() != null ? fv.getCreatedAt().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());

        FolderItemsResponse.FolderRef folderRef = FolderItemsResponse.FolderRef.builder()
                .id(folder.getUuid())
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
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        FavoriteFolder folder = favoriteFolderRepository.findByUuid(req.getFolderId())
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        Long entityId = resolveEntityId(req.getEntityType(), req.getEntityId());
        Favorite favorite = Favorite.builder()
                .folderId(folder.getId())
                .entityType(Favorite.EntityType.valueOf(req.getEntityType()))
                .entityId(entityId)
                .note(req.getNote())
                .build();
        favoriteRepository.save(favorite);
    }

    private Long resolveEntityId(String entityType, String entityUuid) {
        return switch (entityType) {
            case "word" -> wordRepository.findByUuid(entityUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Entity", entityUuid)).getId();
            case "article" -> null;
            default -> null;
        };
    }

    public void removeFavorite(String userId, String favId) {
        Favorite fav = favoriteRepository.findByUuid(favId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        favoriteRepository.delete(fav);
    }

    public void batchDeleteFavorites(String userId, List<String> ids) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (ids == null || ids.isEmpty()) return;
        entityManager.createQuery(
                        "DELETE FROM Favorite fv WHERE fv.uuid IN :uuids AND fv.userId = :userId")
                .setParameter("uuids", ids)
                .setParameter("userId", user.getId())
                .executeUpdate();
    }

    public void batchTagWords(String userId, List<String> wordIds, String tagId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserTag tag = userTagRepository.findByUuid(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTag", tagId));
        if (wordIds == null || wordIds.isEmpty()) return;
        List<UserEntityTag> tags = new ArrayList<>();
        for (String wordUuid : wordIds) {
            Word word = wordRepository.findByUuid(wordUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Word", wordUuid));
            UserEntityTag entityTag = UserEntityTag.builder()
                    .userId(user.getId())
                    .tagId(tag.getId())
                    .entityType("word")
                    .entityId(word.getId())
                    .build();
            tags.add(entityTag);
        }
        userEntityTagRepository.saveAll(tags);
    }
}
