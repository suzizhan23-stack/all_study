import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useWordBookStore = defineStore('wordBooks', () => {
  const books = ref([
    { id: 'wb1', name: '四级单词', description: '大学英语四级核心词汇', difficulty_level: 'CET-4', word_count: 2500, is_active: true, sort_order: 0 },
    { id: 'wb2', name: '六级单词', description: '大学英语六级核心词汇', difficulty_level: 'CET-6', word_count: 3000, is_active: true, sort_order: 1 },
    { id: 'wb3', name: '考研单词', description: '考研英语核心词汇', difficulty_level: '考研', word_count: 3500, is_active: true, sort_order: 2 },
  ])

  const strategies = ref([
    { id: 's1', name: '完全随机', description: '从单词本中完全随机选取', type: 'random', sort_order: 0 },
    { id: 's2', name: '字母顺序', description: '按照首字母 A→Z 顺序选取', type: 'alphabetical', sort_order: 1 },
    { id: 's3', name: '按词性+字母序', description: '选定词性后按字母顺序选取', type: 'pos_alphabetical', sort_order: 2 },
    { id: 's4', name: '按词性+随机', description: '选定词性后随机选取', type: 'pos_random', sort_order: 3 },
    { id: 's5', name: '难度递增', description: '从简单到困难顺序选取', type: 'difficulty_asc', sort_order: 4 },
    { id: 's6', name: '难度递减', description: '从困难到简单顺序选取', type: 'difficulty_desc', sort_order: 5 },
  ])

  const posCategories = ['动词', '名词', '形容词', '副词', '介词', '代词', '连词', '冠词', '数词']
  const posMap = {
    '动词': ['vt.', 'vi.', 'v.'],
    '名词': ['n.'],
    '形容词': ['adj.'],
    '副词': ['adv.'],
    '介词': ['prep.'],
    '代词': ['pron.'],
    '连词': ['conj.'],
    '冠词': ['art.'],
    '数词': ['num.'],
  }

  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

  const activeBook = computed(() => books.value.find(b => b.is_active))

  function getStrategyName(type) {
    const s = strategies.value.find(st => st.type === type)
    return s ? s.name : type
  }

  return { books, strategies, posCategories, posMap, letters, activeBook, getStrategyName }
})
