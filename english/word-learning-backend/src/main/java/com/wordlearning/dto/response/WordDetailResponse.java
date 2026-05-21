package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordDetailResponse {
    private String id;
    private String word;
    private String phoneticUk;
    private String phoneticUs;
    private String audioUk;
    private String audioUs;
    private String pos;
    private String meaningCn;
    private String etymologyCn;
    private String source;
    private int difficulty;
    private int frequency;
    private String firstLetter;
    private List<DefinitionDTO> definitions;
    private List<CollocationDTO> collocations;
    private List<PrepPatternDTO> prepPatterns;
    private List<ExampleDTO> examples;
    private RelationDTO relations;
    private UserDataDTO userData;
    private List<ArticleRefDTO> relatedArticles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DefinitionDTO {
        private String id;
        private String meaningEn;
        private String meaningCn;
        private int sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CollocationDTO {
        private String id;
        private String collocation;
        private String translation;
        private int frequency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrepPatternDTO {
        private String id;
        private String pattern;
        private String translation;
        private String preposition;
        private int frequency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExampleDTO {
        private String id;
        private String sentenceEn;
        private String sentenceCn;
        private String sourceType;
        private String sourceDetail;
        private int frequency;
        private double rating;
        private int ratingCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelationDTO {
        private List<WordRefDTO> synonyms;
        private List<WordRefDTO> antonyms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WordRefDTO {
        private String wordId;
        private String word;
        private String meaningCn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDataDTO {
        private int stage;
        private int confidence;
        private String nextReview;
        private int reviewCount;
        private int consecutiveCorrect;
        private Integer frequency;
        private List<FavoriteRefDTO> favorites;
        private NoteDTO notes;
        private List<TagDTO> tags;
        private Integer rating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FavoriteRefDTO {
        private String folderId;
        private String folderName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoteDTO {
        private String id;
        private String content;
        private boolean isPrivate;
        private String updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TagDTO {
        private String id;
        private String tag;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleRefDTO {
        private String id;
        private String title;
        private String snippet;
    }
}
