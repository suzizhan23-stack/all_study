import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWordStore = defineStore('words', () => {
  const sampleWord = {
    id: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    word: 'abandon',
    pos: 'vt.',
    phonetic_uk: '/əˈbændən/',
    phonetic_us: '/əˈbændən/',
    meaning_cn: '放弃；遗弃；抛弃',
    etymology_cn: '源自古法语 abandoner，意为"置于控制之下"',
    source: 'CET-4',
    difficulty: 2,
    frequency: 80,
    stage: 2,
    confidence: 3,
    review_count: 5,
    consecutive_correct: 3,
    ease_factor: 2.50,
    interval_days: 6,
    next_review: '2026-05-27',
  }

  const sampleDefinitions = [
    { id: 'd1', meaning_en: 'to leave completely and finally; to give up', meaning_cn: '放弃；遗弃；抛弃' },
  ]

  const sampleCollocations = [
    { id: 'c1', collocation: 'abandon hope', translation: '放弃希望', frequency: 5 },
    { id: 'c2', collocation: 'abandon ship', translation: '弃船', frequency: 4 },
    { id: 'c3', collocation: 'abandon a plan', translation: '放弃计划', frequency: 3 },
  ]

  const samplePrepPatterns = [
    { id: 'p1', pattern: 'abandon sth to sb', translation: '把某物丢给某人', preposition: 'to' },
    { id: 'p2', pattern: 'abandon sth for sth', translation: '放弃A选择B', preposition: 'for' },
  ]

  const sampleExamples = [
    { id: 'e1', sentence_en: 'The captain ordered the crew to abandon the sinking ship.', sentence_cn: '船长命令船员弃船。', source_type: 'CET46', source_detail: 'CET-4 2019-06', frequency: 5 },
    { id: 'e2', sentence_en: 'He abandoned his research after years of fruitless effort.', sentence_cn: '经过多年努力，他放弃了研究。', source_type: 'KAOYAN', source_detail: '考研英语 2018', frequency: 4 },
    { id: 'e3', sentence_en: 'Don\'t abandon your dreams just because of one setback.', sentence_cn: '不要因为一次挫折就放弃梦想。', source_type: 'COMMON', source_detail: null, frequency: 4 },
  ]

  const sampleRelations = {
    synonyms: ['desert', 'give up', 'relinquish', 'forsake'],
    antonyms: ['keep', 'retain', 'maintain'],
  }

  const searchResults = ref([])
  const searchHistory = ref([
    { query: 'abandon', searched_at: '2026-05-21 13:20' },
    { query: 'remarkable', searched_at: '2026-05-20' },
    { query: 'contribution', searched_at: '2026-05-19' },
  ])

  function searchWords(query) {
    if (query.toLowerCase() === 'abandon' || query.toLowerCase().startsWith('aban')) {
      searchResults.value = [sampleWord]
    } else {
      searchResults.value = []
    }
  }

  return {
    sampleWord, sampleDefinitions, sampleCollocations,
    samplePrepPatterns, sampleExamples, sampleRelations,
    searchResults, searchHistory, searchWords,
  }
})
