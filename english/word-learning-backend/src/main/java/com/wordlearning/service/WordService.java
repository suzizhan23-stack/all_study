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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final UserRepository userRepository;
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
    private final UserDailyPlanEntryRepository userDailyPlanEntryRepository;
    private final UserEntityTagRepository userEntityTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public WordDetailResponse getWordDetail(String userId, String wordId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        Long uid = user.getId();
        Long wid = word.getId();

        List<WordDetailResponse.DefinitionDTO> definitionDTOs = definitionRepository
                .findByWordIdOrderBySortOrder(wid).stream()
                .map(d -> WordDetailResponse.DefinitionDTO.builder()
                        .id(d.getUuid())
                        .meaningEn(d.getMeaningEn())
                        .meaningCn(d.getMeaningCn())
                        .sortOrder(d.getSortOrder())
                        .build())
                .toList();

        List<WordDetailResponse.CollocationDTO> collocationDTOs = collocationRepository
                .findByWordIdOrderByFrequencyDesc(wid).stream()
                .map(c -> WordDetailResponse.CollocationDTO.builder()
                        .id(c.getUuid())
                        .collocation(c.getCollocation())
                        .translation(c.getTranslation())
                        .frequency(c.getFrequency())
                        .build())
                .toList();

        List<WordDetailResponse.PrepPatternDTO> prepPatternDTOs = prepPatternRepository
                .findByWordIdOrderByFrequencyDesc(wid).stream()
                .map(p -> WordDetailResponse.PrepPatternDTO.builder()
                        .id(p.getUuid())
                        .pattern(p.getPattern())
                        .translation(p.getTranslation())
                        .preposition(p.getPreposition())
                        .frequency(p.getFrequency())
                        .build())
                .toList();

        List<WordDetailResponse.ExampleDTO> exampleDTOs = exampleRepository
                .findByWordIdOrderByFrequencyDesc(wid).stream()
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
                            .id(e.getUuid())
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

        List<WordRelation> relations = wordRelationRepository.findByWordIdOrRelatedWordId(wid, wid);
        List<WordDetailResponse.WordRefDTO> synonyms = new ArrayList<>();
        List<WordDetailResponse.WordRefDTO> antonyms = new ArrayList<>();
        for (WordRelation rel : relations) {
            Long targetId = rel.getWordId().equals(wid) ? rel.getRelatedWordId() : rel.getWordId();
            if (targetId.equals(wid)) continue;
            Word targetWord = wordRepository.findById(targetId).orElse(null);
            if (targetWord == null) continue;
            WordDetailResponse.WordRefDTO ref = WordDetailResponse.WordRefDTO.builder()
                    .wordId(targetWord.getUuid())
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
                .findByUserIdAndEntityTypeAndEntityId(uid, UserFrequency.EntityType.word, wid)
                .orElse(null);

        var favOpt = favoriteRepository.findByUserIdAndEntityTypeAndEntityId(
                uid, Favorite.EntityType.word, wid);
        List<WordDetailResponse.FavoriteRefDTO> favoriteDTOs = new ArrayList<>();
        favOpt.ifPresent(f -> {
            FavoriteFolder folder = favoriteFolderRepository.findById(f.getFolderId()).orElse(null);
            favoriteDTOs.add(WordDetailResponse.FavoriteRefDTO.builder()
                    .folderId(folder != null ? folder.getUuid() : null)
                    .folderName(folder != null ? folder.getName() : null)
                    .build());
        });

        boolean isKeyPoint = userDailyPlanEntryRepository
                .findByUserIdAndPlanDateAndWordId(uid, LocalDate.now(), wid)
                .map(UserDailyPlanEntry::isKeyPoint)
                .orElse(false);

        var userNotes = userNoteRepository.findByUserIdAndEntityTypeAndEntityId(uid, "word", wid);
        WordDetailResponse.NoteDTO noteDTO = userNotes.stream().findFirst()
                .map(n -> WordDetailResponse.NoteDTO.builder()
                        .id(n.getUuid())
                        .content(n.getContent())
                        .isPrivate(n.isPrivate())
                        .updatedAt(n.getUpdatedAt() != null ? n.getUpdatedAt().toString() : null)
                        .build())
                .orElse(null);

        ContentRating rating = contentRatingRepository
                .findByUserIdAndEntityTypeAndEntityId(uid, "word", wid)
                .orElse(null);

        var userEntityTags = userEntityTagRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(uid)
                        && "word".equals(t.getEntityType())
                        && t.getEntityId().equals(wid))
                .toList();
        List<WordDetailResponse.TagDTO> tagDTOs = userEntityTags.stream()
                .map(t -> {
                    UserTag ut = userTagRepository.findById(t.getTagId()).orElse(null);
                    return WordDetailResponse.TagDTO.builder()
                            .id(ut != null ? ut.getUuid() : null)
                            .tag(ut != null ? ut.getTag() : null)
                            .color(ut != null ? ut.getColor() : null)
                            .build();
                })
                .toList();

        return WordDetailResponse.builder()
                .id(word.getUuid())
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
                        .isKeyPoint(isKeyPoint)
                        .notes(noteDTO)
                        .tags(tagDTOs)
                        .rating(rating != null ? rating.getRating() : null)
                        .build())
                .relatedArticles(new ArrayList<>())
                .build();
    }

    public void setFrequency(String userId, String wordId, int freq) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        UserFrequency uf = userFrequencyRepository
                .findByUserIdAndEntityTypeAndEntityId(user.getId(), UserFrequency.EntityType.word, word.getId())
                .orElse(null);
        if (uf == null) {
            uf = UserFrequency.builder()
                    .userId(user.getId())
                    .entityType(UserFrequency.EntityType.word)
                    .entityId(word.getId())
                    .frequency(freq)
                    .build();
        } else {
            uf.setFrequency(freq);
        }
        userFrequencyRepository.save(uf);
    }

    public void saveNote(String userId, String wordId, NoteRequest req) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        var notes = userNoteRepository.findByUserIdAndEntityTypeAndEntityId(user.getId(), "word", word.getId());
        UserNote note = notes.stream().findFirst().orElse(null);
        if (note == null) {
            note = UserNote.builder()
                    .userId(user.getId())
                    .entityType("word")
                    .entityId(word.getId())
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
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        UserTag tag = userTagRepository.findByUuid(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTag", tagId));
        UserEntityTag entityTag = UserEntityTag.builder()
                .userId(user.getId())
                .tagId(tag.getId())
                .entityType("word")
                .entityId(word.getId())
                .build();
        entityManager.persist(entityTag);
    }

    public void removeTag(String userId, String wordId, String tagId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        UserTag tag = userTagRepository.findByUuid(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTag", tagId));
        entityManager.createQuery(
                "DELETE FROM UserEntityTag t WHERE t.userId = :userId AND t.tagId = :tagId AND t.entityType = :entityType AND t.entityId = :entityId")
                .setParameter("userId", user.getId())
                .setParameter("tagId", tag.getId())
                .setParameter("entityType", "word")
                .setParameter("entityId", word.getId())
                .executeUpdate();
    }

    public void rateWord(String userId, String wordId, int rating) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        ContentRating cr = contentRatingRepository
                .findByUserIdAndEntityTypeAndEntityId(user.getId(), "word", word.getId())
                .orElse(null);
        if (cr == null) {
            cr = ContentRating.builder()
                    .userId(user.getId())
                    .entityType("word")
                    .entityId(word.getId())
                    .rating(rating)
                    .build();
        } else {
            cr.setRating(rating);
        }
        contentRatingRepository.save(cr);
    }

    @Transactional(readOnly = true)
    public List<UserTag> getTags(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userTagRepository.findByUserId(user.getId());
    }

    public UserTag createTag(String userId, String tag, String color) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserTag ut = UserTag.builder()
                .userId(user.getId())
                .tag(tag)
                .color(color)
                .build();
        return userTagRepository.save(ut);
    }
}
