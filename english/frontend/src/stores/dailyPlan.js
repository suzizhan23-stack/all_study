import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function formatDate(offset) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

const richWords = [
  {
    id: 'w1', word: 'abandon', phonetic: '/əˈbændən/', posKey: 'verb', posLabel: '动词',
    meaning: '放弃；遗弃；抛弃',
    collocations: [
      { text: 'abandon hope', translation: '放弃希望', frequency: 5 },
      { text: 'abandon ship', translation: '弃船', frequency: 4 },
    ],
    preps: [
      { pattern: 'abandon sth to sb', translation: '把某物丢给某人', preposition: 'to' },
    ],
  },
  {
    id: 'w2', word: 'contribute', phonetic: '/kənˈtrɪbjuːt/', posKey: 'verb', posLabel: '动词',
    meaning: '贡献；捐献；投稿',
    collocations: [
      { text: 'contribute to', translation: '促成；贡献给', frequency: 8 },
      { text: 'contribute money', translation: '捐款', frequency: 4 },
    ],
    preps: [
      { pattern: 'contribute to sth', translation: '促成某事', preposition: 'to' },
    ],
  },
  {
    id: 'w3', word: 'establish', phonetic: '/ɪˈstæblɪʃ/', posKey: 'verb', posLabel: '动词',
    meaning: '建立；确立；安置',
    collocations: [
      { text: 'establish a company', translation: '成立公司', frequency: 5 },
      { text: 'establish relations', translation: '建立关系', frequency: 4 },
    ],
    preps: [
      { pattern: 'establish sb as sth', translation: '确立某人某身份', preposition: 'as' },
    ],
  },
  {
    id: 'w4', word: 'significance', phonetic: '/sɪɡˈnɪfɪkəns/', posKey: 'noun', posLabel: '名词',
    meaning: '意义；重要性；含义',
    collocations: [
      { text: 'great significance', translation: '重大意义', frequency: 6 },
      { text: 'statistical significance', translation: '统计显著性', frequency: 4 },
    ],
  },
  {
    id: 'w5', word: 'approach', phonetic: '/əˈproʊtʃ/', posKey: 'verb', posLabel: '动词',
    meaning: '接近；靠近；处理',
    collocations: [
      { text: 'approach a problem', translation: '处理问题', frequency: 6 },
      { text: 'approach completion', translation: '接近完成', frequency: 4 },
    ],
    preps: [
      { pattern: 'approach sb about sth', translation: '就某事找某人洽谈', preposition: 'about' },
    ],
  },
  {
    id: 'w6', word: 'remarkable', phonetic: '/rɪˈmɑːrkəbl/', posKey: 'adj', posLabel: '形容词',
    meaning: '显著的；非凡的；引人注目的',
    collocations: [
      { text: 'remarkable achievement', translation: '显著成就', frequency: 5 },
    ],
  },
  {
    id: 'w7', word: 'approximately', phonetic: '/əˈprɑːksɪmətli/', posKey: 'adv', posLabel: '副词',
    meaning: '大约；近似地',
    collocations: [
      { text: 'approximately equal', translation: '近似相等', frequency: 4 },
    ],
  },
  {
    id: 'w8', word: 'hypothesis', phonetic: '/haɪˈpɑːθəsɪs/', posKey: 'noun', posLabel: '名词',
    meaning: '假说；假设；前提',
    collocations: [
      { text: 'working hypothesis', translation: '工作假说', frequency: 5 },
    ],
  },
  {
    id: 'w9', word: 'demonstrate', phonetic: '/ˈdemənstreɪt/', posKey: 'verb', posLabel: '动词',
    meaning: '证明；示范；演示；游行',
    collocations: [
      { text: 'demonstrate the effectiveness', translation: '证明有效性', frequency: 5 },
    ],
    preps: [
      { pattern: 'demonstrate sth to sb', translation: '向某人演示某物', preposition: 'to' },
    ],
  },
  {
    id: 'w10', word: 'fundamental', phonetic: '/ˌfʌndəˈmentl/', posKey: 'adj', posLabel: '形容词',
    meaning: '基本的；根本的；基础的',
    collocations: [
      { text: 'fundamental principle', translation: '基本原则', frequency: 6 },
    ],
  },
  {
    id: 'w11', word: 'persuade', phonetic: '/pərˈsweɪd/', posKey: 'verb', posLabel: '动词',
    meaning: '说服；劝说；使相信',
    collocations: [
      { text: 'persuade sb to do', translation: '说服某人做某事', frequency: 7 },
    ],
    preps: [
      { pattern: 'persuade sb of sth', translation: '使某人确信', preposition: 'of' },
    ],
  },
  {
    id: 'w12', word: 'consequently', phonetic: '/ˈkɑːnsɪkwentli/', posKey: 'adv', posLabel: '副词',
    meaning: '因此；所以；结果',
    collocations: [
      { text: 'consequently, the result', translation: '因此，结果', frequency: 5 },
    ],
  },
  {
    id: 'w13', word: 'generate', phonetic: '/ˈdʒenəreɪt/', posKey: 'verb', posLabel: '动词',
    meaning: '产生；生成；引起',
    collocations: [
      { text: 'generate electricity', translation: '发电', frequency: 5 },
      { text: 'generate revenue', translation: '创收', frequency: 4 },
    ],
    preps: [
      { pattern: 'generate sth from sth', translation: '从…产生…', preposition: 'from' },
    ],
  },
  {
    id: 'w14', word: 'significant', phonetic: '/sɪɡˈnɪfɪkənt/', posKey: 'adj', posLabel: '形容词',
    meaning: '重要的；有意义的；显著的',
    collocations: [
      { text: 'significant difference', translation: '显著差异', frequency: 6 },
      { text: 'significant impact', translation: '重大影响', frequency: 5 },
    ],
  },
]

export const useDailyPlanStore = defineStore('dailyPlan', () => {
  const plans = ref(initDefaultPlans())
  const selectedDate = ref(todayStr())

  const today = computed(() => todayStr())

  const availableDates = computed(() => {
    const dates = Object.keys(plans.value).sort().reverse()
    if (!dates.includes(todayStr())) dates.unshift(todayStr())
    return dates
  })

  const currentPlan = computed(() => {
    return plans.value[selectedDate.value] || []
  })

  const currentPlanCount = computed(() => currentPlan.value.length)

  function lookupWord(name) {
    return richWords.find(w => w.word === name)
  }

  function addWord(name) {
    const existing = richWords.find(w => w.word === name)
    if (!existing) return false
    const date = todayStr()
    if (!plans.value[date]) plans.value[date] = []
    if (plans.value[date].some(w => w.word === name)) return false
    plans.value[date].push({ ...existing })
    savePlans()
    return true
  }

  function removeWord(date, wordId) {
    if (!plans.value[date]) return
    plans.value[date] = plans.value[date].filter(w => w.id !== wordId)
    if (plans.value[date].length === 0) delete plans.value[date]
    savePlans()
  }

  function setSelectedDate(date) {
    selectedDate.value = date
  }

  function getWordsForDate(date) {
    return plans.value[date] || []
  }

  function wordPlanCount(wordName) {
    let count = 0
    for (const date in plans.value) {
      if (plans.value[date].some(w => w.word === wordName)) count++
    }
    return count
  }

  function isInTodayPlan(wordName) {
    const todayWords = plans.value[todayStr()]
    return todayWords ? todayWords.some(w => w.word === wordName) : false
  }

  function initDefaultPlans() {
    try {
      const raw = localStorage.getItem('dailyPlans')
      if (raw) return JSON.parse(raw)
    } catch {}
    const defaultWords = ['abandon', 'contribute', 'establish', 'significance', 'approach', 'remarkable', 'hypothesis', 'demonstrate']
    const today = todayStr()
    const plans = {}
    plans[today] = defaultWords.map((name, i) => {
      const w = richWords.find(r => r.word === name)
      return w ? { ...w, id: w.id } : null
    }).filter(Boolean)
    return plans
  }

  function savePlans() {
    try {
      localStorage.setItem('dailyPlans', JSON.stringify(plans.value))
    } catch {}
  }

  return {
    plans, selectedDate,
    today, availableDates, currentPlan, currentPlanCount,
    lookupWord, addWord, removeWord,
    setSelectedDate, getWordsForDate,
    wordPlanCount, isInTodayPlan,
  }
})
