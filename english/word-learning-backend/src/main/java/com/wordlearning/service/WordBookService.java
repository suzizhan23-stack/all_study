package com.wordlearning.service;

import com.wordlearning.dto.response.*;
import com.wordlearning.entity.*;
import com.wordlearning.exception.ResourceNotFoundException;
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
public class WordBookService {

    private final UserRepository userRepository;
    private final WordBookRepository wordBookRepository;
    private final WordBookEntryRepository wordBookEntryRepository;
    private final WordRepository wordRepository;
    private final UserDailyPlanEntryRepository userDailyPlanEntryRepository;
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final StudyStrategyRepository studyStrategyRepository;
    private final CollocationRepository collocationRepository;
    private final PrepPatternRepository prepPatternRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public WordBookListResponse getWordBooks(String difficultyLevel) {
        List<WordBook> books;
        if (difficultyLevel != null && !difficultyLevel.isBlank()) {
            books = entityManager.createQuery(
                            "SELECT wb FROM WordBook wb WHERE wb.difficultyLevel = :level ORDER BY wb.sortOrder", WordBook.class)
                    .setParameter("level", difficultyLevel)
                    .getResultList();
        } else {
            books = entityManager.createQuery(
                            "SELECT wb FROM WordBook wb ORDER BY wb.sortOrder", WordBook.class)
                    .getResultList();
        }

        List<WordBookListResponse.BookItem> items = books.stream()
                .map(b -> {
                    long count = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM WordBookEntry e WHERE e.wordBookId = :bookId", Long.class)
                            .setParameter("bookId", b.getId())
                            .getSingleResult();
                    return WordBookListResponse.BookItem.builder()
                            .id(b.getUuid())
                            .name(b.getName())
                            .description(b.getDescription())
                            .difficultyLevel(b.getDifficultyLevel())
                            .wordCount((int) count)
                            .sortOrder(b.getSortOrder())
                            .isActive(b.isActive())
                            .build();
                })
                .collect(Collectors.toList());

        return WordBookListResponse.builder().books(items).build();
    }

    @Transactional(readOnly = true)
    public WordBookWordsResponse getWordBookWords(String userId, String bookId, String pos, String letter,
                                                   String search, int page, int size) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        WordBook book = wordBookRepository.findByUuid(bookId)
                .orElseThrow(() -> new RuntimeException("Word book not found"));

        StringBuilder jpql = new StringBuilder(
                "SELECT w FROM Word w WHERE w.id IN (SELECT e.wordId FROM WordBookEntry e WHERE e.wordBookId = :bookId)");
        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("bookId", book.getId());

        if (pos != null && !pos.isBlank()) {
            List<String> posList = Arrays.stream(pos.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .collect(Collectors.toList());
            if (!posList.isEmpty()) {
                whereClause.append(" AND w.pos IN :posList");
                params.put("posList", posList);
            }
        }

        if (letter != null && !letter.isBlank()) {
            whereClause.append(" AND w.firstLetter = :letter");
            params.put("letter", letter.toUpperCase());
        }

        if (search != null && !search.isBlank()) {
            whereClause.append(" AND (w.word LIKE :search OR w.meaningCn LIKE :search)");
            params.put("search", "%" + search + "%");
        }

        jpql.append(whereClause);
        jpql.append(" ORDER BY w.word");

        String countJpql = "SELECT COUNT(w) FROM Word w WHERE w.id IN (SELECT e.wordId FROM WordBookEntry e WHERE e.wordBookId = :bookId)"
                + whereClause;
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        TypedQuery<Word> query = entityManager.createQuery(jpql.toString(), Word.class);
        params.forEach(query::setParameter);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        List<Word> words = query.getResultList();

        List<WordBookWordsResponse.WordPreview> previews = words.stream()
                .map(w -> {
                    boolean inPlan = isWordInPlan(user.getId(), w.getId());
                    boolean completed = isWordCompleted(user.getId(), w.getId());
                    return WordBookWordsResponse.WordPreview.builder()
                            .id(w.getUuid())
                            .word(w.getWord())
                            .pos(w.getPos())
                            .meaningCn(w.getMeaningCn())
                            .firstLetter(w.getFirstLetter())
                            .difficulty(w.getDifficulty())
                            .frequency(w.getFrequency())
                            .isInPlan(inPlan)
                            .isCompleted(completed)
                            .build();
                })
                .collect(Collectors.toList());

        WordBookWordsResponse.BookRef bookRef = WordBookWordsResponse.BookRef.builder()
                .id(book.getUuid())
                .name(book.getName())
                .build();

        List<WordBookWordsResponse.PosCategory> posCategories = getPosCategoriesFromWords(book.getId());

        WordBookWordsResponse.Filters filters = WordBookWordsResponse.Filters.builder()
                .posCategories(posCategories)
                .letters(generateLetters())
                .build();

        return WordBookWordsResponse.builder()
                .book(bookRef)
                .filters(filters)
                .words(previews)
                .pagination(new PageResponse.Pagination(page, size, total, (int) Math.ceil((double) total / size)))
                .build();
    }

    private boolean isWordInPlan(Long userId, Long wordId) {
        long udCount = entityManager.createQuery(
                        "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.wordId = :wordId", Long.class)
                .setParameter("userId", userId)
                .setParameter("wordId", wordId)
                .getSingleResult();
        if (udCount > 0) return true;
        long dpCount = entityManager.createQuery(
                        "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.wordId = :wordId", Long.class)
                .setParameter("userId", userId)
                .setParameter("wordId", wordId)
                .getSingleResult();
        return dpCount > 0;
    }

    private boolean isWordCompleted(Long userId, Long wordId) {
        long udCount = entityManager.createQuery(
                        "SELECT COUNT(e) FROM UserDailyPlanEntry e WHERE e.userId = :userId AND e.wordId = :wordId AND e.isCompleted = true", Long.class)
                .setParameter("userId", userId)
                .setParameter("wordId", wordId)
                .getSingleResult();
        if (udCount > 0) return true;
        long dpCount = entityManager.createQuery(
                        "SELECT COUNT(e) FROM DailyPlanItem e WHERE e.userId = :userId AND e.wordId = :wordId AND e.isCompleted = true", Long.class)
                .setParameter("userId", userId)
                .setParameter("wordId", wordId)
                .getSingleResult();
        return dpCount > 0;
    }

    private List<WordBookWordsResponse.PosCategory> getPosCategoriesFromWords(Long bookId) {
        Map<String, List<String>> categoryMap = new LinkedHashMap<>();
        categoryMap.put("名词", Arrays.asList("n.", "n", "noun"));
        categoryMap.put("动词", Arrays.asList("v.", "v", "verb", "vt.", "vi.", "vt. & vi.", "vi. & vt."));
        categoryMap.put("形容词", Arrays.asList("adj.", "adj", "adjective"));
        categoryMap.put("副词", Arrays.asList("adv.", "adv", "adverb"));
        categoryMap.put("代词", Arrays.asList("pron.", "pron", "pronoun"));
        categoryMap.put("介词", Arrays.asList("prep.", "prep", "preposition"));
        categoryMap.put("连词", Arrays.asList("conj.", "conj", "conjunction"));
        categoryMap.put("冠词", Arrays.asList("art.", "art", "article"));
        categoryMap.put("感叹词", Arrays.asList("interj.", "interj", "interjection"));
        categoryMap.put("数词", Arrays.asList("num.", "num", "numeral"));
        categoryMap.put("其他", Arrays.asList("phrase", "other"));

        List<WordBookWordsResponse.PosCategory> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : categoryMap.entrySet()) {
            long count = entityManager.createQuery(
                            "SELECT COUNT(DISTINCT w.id) FROM Word w WHERE w.pos IN :posList " +
                                    "AND w.id IN (SELECT e.wordId FROM WordBookEntry e WHERE e.wordBookId = :bookId)", Long.class)
                    .setParameter("bookId", bookId)
                    .setParameter("posList", entry.getValue())
                    .getSingleResult();
            if (count > 0) {
                result.add(WordBookWordsResponse.PosCategory.builder()
                        .label(entry.getKey())
                        .key(entry.getValue().get(0))
                        .posList(entry.getValue())
                        .count(count)
                        .build());
            }
        }
        return result;
    }

    private List<String> generateLetters() {
        List<String> letters = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            letters.add(String.valueOf(c));
        }
        return letters;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPosCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();

        Map<String, Object> all = new LinkedHashMap<>();
        all.put("label", "全部");
        all.put("value", "all");
        categories.add(all);

        addCategory(categories, "名词", "n.", Arrays.asList("n.", "n", "noun"));
        addCategory(categories, "动词", "v.", Arrays.asList("v.", "v", "verb", "vt.", "vi.", "vt. & vi.", "vi. & vt."));
        addCategory(categories, "形容词", "adj.", Arrays.asList("adj.", "adj", "adjective"));
        addCategory(categories, "副词", "adv.", Arrays.asList("adv.", "adv", "adverb"));
        addCategory(categories, "代词", "pron.", Arrays.asList("pron.", "pron", "pronoun"));
        addCategory(categories, "介词", "prep.", Arrays.asList("prep.", "prep", "preposition"));
        addCategory(categories, "连词", "conj.", Arrays.asList("conj.", "conj", "conjunction"));
        addCategory(categories, "冠词", "art.", Arrays.asList("art.", "art", "article"));
        addCategory(categories, "感叹词", "interj.", Arrays.asList("interj.", "interj", "interjection"));
        addCategory(categories, "数词", "num.", Arrays.asList("num.", "num", "numeral"));
        addCategory(categories, "其他", "other", Arrays.asList("phrase", "other"));

        return categories;
    }

    private void addCategory(List<Map<String, Object>> list, String label, String value, List<String> posList) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("posList", posList);
        list.add(item);
    }

    @Transactional(readOnly = true)
    public StrategyListResponse getStrategies() {
        List<StudyStrategy> strategies = studyStrategyRepository.findAll();
        List<StrategyListResponse.StrategyItem> items = strategies.stream()
                .map(s -> StrategyListResponse.StrategyItem.builder()
                        .id(s.getUuid())
                        .name(s.getName())
                        .description(s.getDescription())
                        .type(s.getType().name())
                        .config(s.getConfig())
                        .sortOrder(s.getSortOrder())
                        .build())
                .collect(Collectors.toList());
        return StrategyListResponse.builder().strategies(items).build();
    }

    @Transactional(readOnly = true)
    public StrategyListResponse.StrategyItem getUserDefaultStrategy(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getDefaultStrategyId() == null) return null;
        StudyStrategy strategy = studyStrategyRepository.findById(user.getDefaultStrategyId())
                .orElse(null);
        if (strategy == null) return null;
        return StrategyListResponse.StrategyItem.builder()
                .id(strategy.getUuid())
                .name(strategy.getName())
                .description(strategy.getDescription())
                .type(strategy.getType().name())
                .config(strategy.getConfig())
                .sortOrder(strategy.getSortOrder())
                .build();
    }

    public void setUserDefaultStrategy(String userId, String strategyId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudyStrategy strategy = studyStrategyRepository.findByUuid(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found"));
        user.setDefaultStrategyId(strategy.getId());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public CollocationResponse getCollocations(String wordId, boolean compact, int limit) {
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        List<Collocation> collocations = collocationRepository.findByWordIdOrderByFrequencyDesc(word.getId());
        if (limit > 0 && limit < collocations.size()) {
            collocations = collocations.subList(0, limit);
        }
        List<CollocationResponse.CollocationItem> items = collocations.stream()
                .map(c -> CollocationResponse.CollocationItem.builder()
                        .id(c.getUuid())
                        .collocation(c.getCollocation())
                        .translation(compact ? null : c.getTranslation())
                        .frequency(c.getFrequency())
                        .build())
                .collect(Collectors.toList());
        return CollocationResponse.builder().collocations(items).build();
    }

    @Transactional(readOnly = true)
    public PrepPatternResponse getPrepPatterns(String wordId, boolean compact, int limit) {
        Word word = wordRepository.findByUuid(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));
        List<PrepPattern> patterns = prepPatternRepository.findByWordIdOrderByFrequencyDesc(word.getId());
        if (limit > 0 && limit < patterns.size()) {
            patterns = patterns.subList(0, limit);
        }
        List<PrepPatternResponse.PrepItem> items = patterns.stream()
                .map(p -> PrepPatternResponse.PrepItem.builder()
                        .id(p.getUuid())
                        .pattern(p.getPattern())
                        .translation(compact ? null : p.getTranslation())
                        .preposition(p.getPreposition())
                        .frequency(p.getFrequency())
                        .build())
                .collect(Collectors.toList());
        return PrepPatternResponse.builder().prepPatterns(items).build();
    }
}
