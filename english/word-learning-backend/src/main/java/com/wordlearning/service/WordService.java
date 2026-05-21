package com.wordlearning.service;

import com.wordlearning.dto.request.NoteRequest;
import com.wordlearning.dto.response.WordDetailResponse;
import com.wordlearning.entity.*;
import com.wordlearning.exception.ResourceNotFoundException;
import com.wordlearning.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final DefinitionRepository definitionRepository;
    private final CollocationRepository collocationRepository;
    private final PrepPatternRepository prepPatternRepository;
    private final ExampleRepository exampleRepository;
    private final WordRelationRepository wordRelationRepository;
    private final UserFrequencyRepository userFrequencyRepository;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final UserNoteRepository userNoteRepository;
    private final WordTagRepository wordTagRepository;
    private final ContentRatingRepository contentRatingRepository;
    private final UserTagRepository userTagRepository;
    private final UserEntityTagRepository userEntityTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public WordDetailResponse getWordDetail(String userId, String wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));

        List<WordDetailResponse.DefinitionDTO> definitionDTOs = definitionRepository
                .findByWordIdOrderBySortOrder(wordId).stream()
                .map(d -> WordDetailResponse.DefinitionDTO.builder()
                        .id(d.getId())
                        .meaningEn(d.getMeaningEn())
                        .meaningCn(d.getMeaningCn())
                        .sortOrder(d.getSortOrder())
                        .build())
                .toList();

        List<WordDetailResponse.CollocationDTO> collocationDTOs = collocationRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .map(c -> WordDetailResponse.CollocationDTO.builder()
                        .id(c.getId())
                        .collocation(c.getCollocation())
                        .translation(c.getTranslation())
                        .frequency(c.getFrequency())
                        .build())
                .toList();

        List<WordDetailResponse.PrepPatternDTO> prepPatternDTOs = prepPatternRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .map(p -> WordDetailResponse.PrepPatternDTO.builder()
                        .id(p.getId())
                        .pattern(p.getPattern())
                        .translation(p.getTranslation())
                        .preposition(p.getPreposition())
                        .frequency(p.getFrequency())
                        .build())
                .toList();

        List<WordDetailResponse.ExampleDTO> exampleDTOs = exampleRepository
                .findByWordIdOrderByFrequencyDesc(wordId).stream()
                .map(e -> {
                    Double avg = entityManager.createQuery(
                            "SELECT AVG(cr.rating) FROM ContentRating cr WHERE cr.entityType = 'example' AND cr.entityId = :entityId",
                            Double.class)
                            .setParameter("entityId", e.getId())
                            .getSingleResult();
                    Long count = entityManager.createQuery(
                            "SELECT COUNT(cr) FROM ContentRating cr WHERE cr.entityType = 'example' AND cr.entityId = :entityId",
                            Long.class)
                            .setParameter("entityId", e.getId())
                            .getSingleResult();
                    return WordDetailResponse.ExampleDTO.builder()
                            .id(e.getId())
                            .sentenceEn(e.getSentenceEn())
                            .sentenceCn(e.getSentenceCn())
                            .sourceType(e.getSourceType() != null ? e.getSourceType().name() : null)
                            .sourceDetail(e.getSourceDetail())
                            .frequency(e.getFrequency())
                            .rating(avg != null ? avg : 0.0)
                            .ratingCount(count != null ? count.intValue() : 0)
                            .build();
                })
                .toList();

        List<WordRelation> relations = wordRelationRepository.findByWordIdOrRelatedWordId(wordId, wordId);
        List<WordDetailResponse.WordRefDTO> synonyms = new ArrayList<>();
        List<WordDetailResponse.WordRefDTO> antonyms = new ArrayList<>();
        for (WordRelation rel : relations) {
            String targetId = rel.getWordId().equals(wordId) ? rel.getRelatedWordId() : rel.getWordId();
            if (targetId.equals(wordId)) continue;
            Word targetWord = wordRepository.findById(targetId).orElse(null);
            if (targetWord == null) continue;
            WordDetailResponse.WordRefDTO ref = WordDetailResponse.WordRefDTO.builder()
                    .wordId(targetId)
                    .word(targetWord.getWord())
                    .meaningCn(targetWord.getMeaningCn())
                    .build();
            if (rel.getRelationType() == WordRelation.RelationType.synonym) {
                synonyms.add(ref);
            } else if (rel.getRelationType() == WordRelation.RelationType.antonym) {
                antonyms.add(ref);
            }
        }

        UserFrequency userFreq = userFrequencyRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, UserFrequency.EntityType.word, wordId)
                .orElse(null);

        var favOpt = favoriteRepository.findByUserIdAndEntityTypeAndEntityId(
                userId, Favorite.EntityType.word, wordId);
        List<WordDetailResponse.FavoriteRefDTO> favoriteDTOs = new ArrayList<>();
        favOpt.ifPresent(f -> {
            FavoriteFolder folder = favoriteFolderRepository.findById(f.getFolderId()).orElse(null);
            favoriteDTOs.add(WordDetailResponse.FavoriteRefDTO.builder()
                    .folderId(f.getFolderId())
                    .folderName(folder != null ? folder.getName() : null)
                    .build());
        });

        var userNotes = userNoteRepository.findByUserIdAndEntityTypeAndEntityId(userId, "word", wordId);
        WordDetailResponse.NoteDTO noteDTO = userNotes.stream().findFirst()
                .map(n -> WordDetailResponse.NoteDTO.builder()
                        .id(n.getId())
                        .content(n.getContent())
                        .isPrivate(n.isPrivate())
                        .updatedAt(n.getUpdatedAt() != null ? n.getUpdatedAt().toString() : null)
                        .build())
                .orElse(null);

        ContentRating rating = contentRatingRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, "word", wordId)
                .orElse(null);

        var userEntityTags = userEntityTagRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(userId)
                        && "word".equals(t.getEntityType())
                        && t.getEntityId().equals(wordId))
                .toList();
        List<WordDetailResponse.TagDTO> tagDTOs = userEntityTags.stream()
                .map(t -> {
                    UserTag ut = userTagRepository.findById(t.getTagId()).orElse(null);
                    return WordDetailResponse.TagDTO.builder()
                            .id(t.getTagId())
                            .tag(ut != null ? ut.getTag() : null)
                            .color(ut != null ? ut.getColor() : null)
                            .build();
                })
                .toList();

        return WordDetailResponse.builder()
                .id(word.getId())
                .word(word.getWord())
                .phoneticUk(word.getPhoneticUk())
                .phoneticUs(word.getPhoneticUs())
                .audioUk(word.getAudioUk())
                .audioUs(word.getAudioUs())
                .pos(word.getPos())
                .meaningCn(word.getMeaningCn())
                .etymologyCn(word.getEtymologyCn())
                .source(word.getSource())
                .difficulty(word.getDifficulty())
                .frequency(word.getFrequency())
                .firstLetter(word.getFirstLetter())
                .definitions(definitionDTOs)
                .collocations(collocationDTOs)
                .prepPatterns(prepPatternDTOs)
                .examples(exampleDTOs)
                .relations(WordDetailResponse.RelationDTO.builder().synonyms(synonyms).antonyms(antonyms).build())
                .userData(WordDetailResponse.UserDataDTO.builder()
                        .stage(word.getStage())
                        .confidence(word.getConfidence())
                        .nextReview(word.getNextReview() != null ? word.getNextReview().toString() : null)
                        .reviewCount(word.getReviewCount())
                        .consecutiveCorrect(word.getConsecutiveCorrect())
                        .frequency(userFreq != null ? userFreq.getFrequency() : null)
                        .favorites(favoriteDTOs)
                        .notes(noteDTO)
                        .tags(tagDTOs)
                        .rating(rating != null ? rating.getRating() : null)
                        .build())
                .relatedArticles(new ArrayList<>())
                .build();
    }

    public void setFrequency(String userId, String wordId, int freq) {
        UserFrequency uf = userFrequencyRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, UserFrequency.EntityType.word, wordId)
                .orElse(null);
        if (uf == null) {
            uf = UserFrequency.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .entityType(UserFrequency.EntityType.word)
                    .entityId(wordId)
                    .frequency(freq)
                    .build();
        } else {
            uf.setFrequency(freq);
        }
        userFrequencyRepository.save(uf);
    }

    public void saveNote(String userId, String wordId, NoteRequest req) {
        var notes = userNoteRepository.findByUserIdAndEntityTypeAndEntityId(userId, "word", wordId);
        UserNote note = notes.stream().findFirst().orElse(null);
        if (note == null) {
            note = UserNote.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .entityType("word")
                    .entityId(wordId)
                    .content(req.getContent())
                    .isPrivate(req.getIsPrivate() != null ? req.getIsPrivate() : false)
                    .build();
        } else {
            note.setContent(req.getContent());
            if (req.getIsPrivate() != null) {
                note.setPrivate(req.getIsPrivate());
            }
        }
        userNoteRepository.save(note);
    }

    public void addTag(String userId, String wordId, String tagId) {
        UserEntityTag tag = UserEntityTag.builder()
                .userId(userId)
                .tagId(tagId)
                .entityType("word")
                .entityId(wordId)
                .build();
        entityManager.persist(tag);
    }

    public void removeTag(String userId, String wordId, String tagId) {
        entityManager.createQuery(
                "DELETE FROM UserEntityTag t WHERE t.userId = :userId AND t.tagId = :tagId AND t.entityType = :entityType AND t.entityId = :entityId")
                .setParameter("userId", userId)
                .setParameter("tagId", tagId)
                .setParameter("entityType", "word")
                .setParameter("entityId", wordId)
                .executeUpdate();
    }

    public void rateWord(String userId, String wordId, int rating) {
        ContentRating cr = contentRatingRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, "word", wordId)
                .orElse(null);
        if (cr == null) {
            cr = ContentRating.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .entityType("word")
                    .entityId(wordId)
                    .rating(rating)
                    .build();
        } else {
            cr.setRating(rating);
        }
        contentRatingRepository.save(cr);
    }

    @Transactional(readOnly = true)
    public List<UserTag> getTags(String userId) {
        return userTagRepository.findByUserId(userId);
    }

    public UserTag createTag(String userId, String tag, String color) {
        UserTag ut = UserTag.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tag(tag)
                .color(color)
                .build();
        return userTagRepository.save(ut);
    }
}
