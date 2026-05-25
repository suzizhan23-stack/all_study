<template>
  <div class="learning-page">
    <!-- 学习设置（可折叠） -->
    <div class="card" style="margin-bottom:20px">
      <div class="settings-header" @click="showSettings = !showSettings">
        <h3 style="margin:0">📚 学习设置</h3>
        <span class="settings-toggle">{{ showSettings ? '收起 ▲' : '展开 ▼' }}</span>
      </div>
      <div v-show="showSettings" class="settings-body">
        <div class="start-row">
          <label>单词本：</label>
          <select v-model="selectedBookId" class="input" style="max-width:300px">
            <option value="" disabled>请选择单词本</option>
            <option v-for="b in wbStore.books" :key="b.id" :value="b.id">
              {{ b.name }}（{{ b.word_count }} 词）
            </option>
          </select>
        </div>
        <div class="start-row" style="margin-top:12px">
          <label>学习策略：</label>
          <div class="strategy-options">
            <div
              v-for="s in wbStore.strategies"
              :key="s.id"
              class="strategy-card"
              :class="{ 'strategy-selected': selectedStrategyId === s.id }"
              @click="selectedStrategyId = s.id"
            >
              <div class="strategy-name">{{ s.name }}</div>
              <div class="strategy-desc">{{ s.description }}</div>
            </div>
          </div>
        </div>
        <div class="start-row" style="margin-top:12px;align-items:center">
          <label>每日词数：</label>
          <input v-model.number="dailyCount" class="input" type="number" min="5" max="100" style="max-width:100px" />
          <span style="font-size:14px;color:var(--color-text-secondary);margin-left:8px">词 / 天</span>
          <button v-if="activePlanInfo" class="btn btn-primary btn-sm" style="margin-left:auto" @click="applySettings">应用</button>
          <button v-else class="btn btn-success btn-sm" style="margin-left:auto" @click="startLearning">🚀 开始学习</button>
        </div>
      </div>
    </div>

    <!-- 计划日期选择（始终显示，支持查看历史） -->
    <div class="plan-selector card">
      <div class="plan-selector-row">
        <h3 style="margin:0">📋 学习计划</h3>
        <div class="plan-date-nav">
          <button class="btn btn-sm" :disabled="!prevDate" @click="goDate(-1)">‹ 前一天</button>
          <input v-model="selectedDate" type="date" class="input plan-date-select" @change="onDateChange" />
          <button class="btn btn-sm" :disabled="!nextDate" @click="goDate(1)">后一天 ›</button>
        </div>
      </div>
      <div v-if="activePlanInfo" class="plan-summary">
        <span v-if="isToday" class="badge badge-green">今天</span>
        <span class="badge badge-blue" style="cursor:pointer" @click="showSettings = !showSettings">{{ activePlanInfo.wordBook?.name }}</span>
        <span class="plan-count">{{ planStore.dailyWords.length }} 个单词</span>
        <span class="plan-count" style="margin-left:4px">· 第 {{ activePlanInfo.currentDay }}/{{ activePlanInfo.totalDays }} 天</span>
        <router-link to="/word-books" class="btn btn-sm btn-primary" style="margin-left:auto">浏览单词本</router-link>
      </div>
    </div>

    <!-- 计划为空 -->
    <div v-if="activePlanInfo && planStore.dailyWords.length === 0" class="card empty-state" style="padding:60px;text-align:center">
      <p style="font-size:16px;margin-bottom:12px">
        {{ isToday ? '今天还没有学习计划' : '该日期没有学习计划' }}
      </p>
      <p style="font-size:14px;color:var(--color-text-secondary);margin-bottom:16px">
        {{ isToday ? '去单词本中选择单词加入今天的计划吧' : '选择一个有计划的日期，或回到今天' }}
      </p>
      <button v-if="isToday" class="btn btn-primary" @click="regenerateToday">从单词本生成今日计划</button>
      <button v-else class="btn btn-primary" @click="goToToday()">回到今天</button>
    </div>

    <!-- 词性筛选 -->
    <div v-if="planStore.dailyWords.length > 0" class="learn-filter-bar">
      <button
        v-for="cat in posCategories" :key="cat.key"
        class="btn btn-sm"
        :class="{ 'btn-primary': activePos === cat.key }"
        :style="activePos !== cat.key ? `border-color:${cat.color};color:${cat.color}` : ''"
        @click="activePos = activePos === cat.key ? '' : cat.key"
      >{{ cat.label }}</button>
      <span class="filter-count">{{ filteredWords.length }}/{{ planStore.dailyWords.length }}</span>
      <button class="btn btn-sm" style="margin-left:auto" @click="shuffleWords">🔀 打乱</button>
      <button v-if="!isToday" class="btn btn-sm btn-success" @click="addAllToToday">一键加入今日计划</button>
    </div>

    <!-- 单词卡片列表 -->
    <div v-if="planStore.dailyWords.length > 0" class="learn-grid">
      <div
        v-for="(item, idx) in filteredWords" :key="item.id"
        class="card learn-card"
        :class="'pos-' + item.posKey"
      >
        <div class="card-index">{{ idx + 1 }}/{{ filteredWords.length }}</div>
        <div class="card-head" :class="'head-' + item.posKey" @click="toggleReveal('m-' + item.id)" style="cursor:pointer">
          <div class="card-head-left">
            <div class="card-word">{{ item.word }}</div>
            <div class="card-phonetic phonetic">{{ item.phonetic }}</div>
            <div v-if="revealedIds.has('m-' + item.id)" class="card-meaning-inline">{{ item.meaningCn || item.meaning_cn || '' }}</div>
          </div>
          <span class="pos-badge" :class="'badge-' + item.posKey">{{ item.posLabel }}</span>
        </div>

        <div v-if="item.collocations && item.collocations.length" class="card-section">
          <div class="section-title">
            <span>常用搭配</span>
            <span class="section-count">{{ item.collocations.length }} 个</span>
          </div>
          <div class="colloc-list">
            <div v-for="c in item.collocations" :key="c.text" class="colloc-item" :class="{ 'colloc-revealed': revealedIds.has('lc-' + c.text) }" @click="toggleReveal('lc-' + c.text)">
              <span class="colloc-text">{{ c.text }}</span>
              <span v-if="revealedIds.has('lc-' + c.text)" class="colloc-trans">{{ c.translation }}</span>
            </div>
          </div>
        </div>

        <div v-if="item.preps && item.preps.length" class="card-section">
          <div class="section-title">
            <span>介词短语</span>
            <span class="section-count">{{ item.preps.length }} 个</span>
          </div>
          <div class="prep-list">
            <div v-for="p in item.preps" :key="p.pattern" class="prep-item" :class="{ 'prep-revealed': revealedIds.has('lp-' + p.pattern) }" @click="toggleReveal('lp-' + p.pattern)">
              <span class="prep-pattern">{{ p.pattern }}</span>
              <span v-if="revealedIds.has('lp-' + p.pattern)" class="prep-trans">{{ p.translation }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer-actions">
          <router-link :to="`/word/${item.wordId}`" class="card-footer-link">查看完整详情 →</router-link>
          <button class="card-footer-keypoint" :class="{ 'is-keypoint': item.isKeyPoint }" @click="toggleKeyPoint(item)">{{ item.isKeyPoint ? '⭐' : '☆' }} 重点</button>
          <button class="card-footer-remove" @click="removeFromPlan(item.id)">🗑️ 移出计划</button>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast -->
  <div v-if="toast.show" class="toast" :class="'toast-' + toast.type" @click="toast.show = false">
    {{ toast.msg }}
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDailyPlanStore } from '../stores/dailyPlan'
import { useWordBookStore } from '../stores/wordBooks'
import { planApi } from '../api'

const planStore = useDailyPlanStore()
const wbStore = useWordBookStore()

const activePos = ref('')
const selectedDate = ref('')
const loading = ref(false)
const activePlanInfo = ref(null)
const showSettings = ref(true)
const selectedBookId = ref('')
const selectedStrategyId = ref('')
const dailyCount = ref(10)
const revealedIds = ref(new Set())

const toast = ref({ show: false, msg: '', type: 'success' })
let toastTimer = null
function showToast(msg, type = 'success') {
  toast.value = { show: true, msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 2000)
}

function toggleReveal(id) {
  const s = revealedIds.value
  if (s.has(id)) s.delete(id); else s.add(id)
  revealedIds.value = new Set(s)
}

const posNormalize = {
  'v.': 'verb', 'v': 'verb', 'verb': 'verb',
  'vt.': 'verb', 'vi.': 'verb',
  'vt. & vi.': 'verb', 'vi. & vt.': 'verb',
  'n.': 'noun', 'n': 'noun', 'noun': 'noun',
  'adj.': 'adj', 'adj': 'adj', 'adjective': 'adj',
  'adv.': 'adv', 'adv': 'adv', 'adverb': 'adv',
  'prep.': 'prep', 'prep': 'prep', 'preposition': 'prep',
}
const knownPos = new Set(['verb', 'noun', 'adj', 'adv', 'prep'])
const posLabels = { verb: '动词', noun: '名词', adj: '形容词', adv: '副词', prep: '介词' }

const posCategories = [
  { key: 'keypoint', label: '⭐ 重点', color: '#ef4444' },
  { key: 'verb', label: '动词', color: '#3b82f6' },
  { key: 'noun', label: '名词', color: '#10b981' },
  { key: 'adj', label: '形容词', color: '#f59e0b' },
  { key: 'adv', label: '副词', color: '#8b5cf6' },
  { key: 'prep', label: '介词', color: '#06b6d4' },
]

const today = computed(() => new Date().toISOString().slice(0, 10))

const isToday = computed(() => selectedDate.value === today.value)


const prevDate = computed(() => {
  if (!selectedDate.value) return null
  const d = new Date(selectedDate.value)
  if (isNaN(d.getTime())) return null
  d.setDate(d.getDate() - 1)
  return d.toISOString().slice(0, 10)
})

const nextDate = computed(() => {
  if (!selectedDate.value) return null
  const d = new Date(selectedDate.value)
  if (isNaN(d.getTime())) return null
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
})

const dailyWordsMapped = computed(() => {
  return (planStore.dailyWords || []).map(w => {
    const rawPos = w.pos || ''
    const normPos = knownPos.has(rawPos) ? rawPos : (posNormalize[rawPos] || 'other')
    return {
      ...w,
      phonetic: w.phoneticUk || w.phonetic || '',
      posKey: normPos,
      posLabel: posLabels[normPos] || w.posLabel || w.pos || '',
      isKeyPoint: w.isKeyPoint || w.keyPoint || false,
      collocations: w.collocations || [],
      preps: w.preps || [],
    }
  })
})

const filteredWords = computed(() => {
  let source = wordOrder.value || dailyWordsMapped.value
  if (!activePos.value) return source
  if (activePos.value === 'keypoint') return source.filter(w => w.isKeyPoint)
  return source.filter(w => w.posKey === activePos.value)
})

const wordOrder = ref(null)

function shuffleWords() {
  const arr = dailyWordsMapped.value.slice()
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]]
  }
  wordOrder.value = arr
}

async function fetchPlanData(date) {
  loading.value = true
  wordOrder.value = null
  try {
    await Promise.all([
      planStore.fetchDailyDates(30),
      planStore.fetchDailyWords(date),
    ])
  } finally {
    loading.value = false
  }
}

async function startLearning() {
  if (!selectedBookId.value) return
  const strategyId = selectedStrategyId.value || wbStore.strategies[0]?.id
  if (!strategyId) return
  try {
    activePlanInfo.value = await planApi.setCurrentWordBook({
      wordBookId: selectedBookId.value,
      strategyId,
      dailyCount: dailyCount.value || 10,
    })
    showSettings.value = false
    await fetchPlanData(today.value)
  } catch (e) {
    // ignore
  }
}

async function applySettings() {
  if (!selectedBookId.value) return
  const strategyId = selectedStrategyId.value || wbStore.strategies[0]?.id
  if (!strategyId) return
  try {
    activePlanInfo.value = await planApi.setCurrentWordBook({
      wordBookId: selectedBookId.value,
      strategyId,
      dailyCount: dailyCount.value || 10,
    })
    showSettings.value = false
    await fetchPlanData(today.value)
  } catch (e) {
    // ignore
  }
}

async function regenerateToday() {
  if (!activePlanInfo.value?.wordBook?.id) return
  const strategy = wbStore.strategies[0]
  if (!strategy) return
  try {
    await planApi.setCurrentWordBook({
      wordBookId: activePlanInfo.value.wordBook.id,
      strategyId: strategy.id,
      dailyCount: activePlanInfo.value.dailyCount || 10,
    })
    await fetchPlanData(today.value)
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  selectedDate.value = today.value
  await Promise.all([
    wbStore.fetchBooks(),
    wbStore.fetchStrategies(),
  ])
  activePlanInfo.value = await planApi.getActive()
  showSettings.value = !activePlanInfo.value
  if (activePlanInfo.value?.wordBook?.id) {
    selectedBookId.value = activePlanInfo.value.wordBook.id
  } else if (wbStore.books.length) {
    selectedBookId.value = wbStore.books[0].id
  }
  if (wbStore.strategies.length) {
    selectedStrategyId.value = wbStore.strategies[0].id
  }
  await fetchPlanData(selectedDate.value)
})

function onDateChange() {
  fetchPlanData(selectedDate.value)
}

function goDate(dir) {
  const target = dir === -1 ? prevDate.value : nextDate.value
  if (target) {
    selectedDate.value = target
    fetchPlanData(target)
  }
}

function goToToday() {
  selectedDate.value = today.value
  fetchPlanData(selectedDate.value)
}

async function addAllToToday() {
  const words = planStore.dailyWords
  const filtered = filteredWords.value
  const target = filtered.length < words.length ? filtered : words
  if (!target.length) return
  const todayStr = today.value
  const wordIds = target.map(w => w.id || w.wordId).filter(Boolean)
  try {
    await planApi.batchAddEntries({ wordIds, planDate: todayStr })
    showToast(`已将 ${wordIds.length} 个单词加入今日计划`, 'success')
  } catch {
    showToast('加入失败', 'warning')
  }
}

function removeFromPlan(wordId) {
  planStore.deleteEntry(wordId)
}

async function toggleKeyPoint(item) {
  try {
    await planApi.toggleKeyPoint(item.id)
    item.isKeyPoint = !item.isKeyPoint
    showToast(item.isKeyPoint ? '已加入今日重点' : '已移出今日重点')
  } catch {
    showToast('操作失败', 'warning')
  }
}
</script>

<style scoped>
.plan-selector { margin-bottom: 20px; padding: 16px 20px; }
.plan-selector-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.plan-date-nav { display: flex; align-items: center; gap: 8px; }
.plan-date-select { min-width: 180px; text-align: center; }
.plan-summary { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
.plan-count { font-size: 14px; color: var(--color-text-secondary); }

.start-row { display: flex; gap: 12px; }
.start-row > label { min-width: 80px; font-size: 14px; font-weight: 500; padding-top: 6px; }
.strategy-options { display: flex; flex-wrap: wrap; gap: 8px; }
.strategy-card {
  padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-sm);
  cursor: pointer; transition: .15s; min-width: 140px;
}
.strategy-card:hover { border-color: var(--color-primary); }
.strategy-selected { border-color: var(--color-primary); background: var(--color-primary-light); }
.strategy-name { font-size: 14px; font-weight: 600; }
.strategy-desc { font-size: 12px; color: var(--color-text-secondary); }

.settings-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; user-select: none; }
.settings-toggle { font-size: 13px; color: var(--color-text-secondary); }
.settings-body { margin-top: 16px; }

.learn-filter-bar { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; margin-bottom: 20px; }
.filter-count { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }

.learn-grid {
  columns: 400px 2;
  column-gap: 24px;
}
@media (max-width: 768px) {
  .learn-grid { columns: 1; }
  .plan-selector-row { flex-direction: column; align-items: stretch; }
  .plan-date-nav { justify-content: center; }
}
.learn-card {
  break-inside: avoid;
  margin-bottom: 24px;
}

.learn-card { padding: 0; overflow: hidden; transition: .2s; border-radius: var(--radius-lg, 12px); position: relative; }
.learn-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); }
.card-index { position: absolute; top: 8px; right: 10px; font-size: 12px; color: var(--color-text-secondary); background: var(--color-bg); padding: 2px 8px; border-radius: 10px; z-index: 1; }

.card-head { padding: 24px 28px 16px; display: flex; justify-content: space-between; align-items: flex-start; }
.head-verb { background: linear-gradient(135deg, #eff6ff, #dbeafe); border-bottom: 2px solid #3b82f6; }
.head-noun { background: linear-gradient(135deg, #ecfdf5, #d1fae5); border-bottom: 2px solid #10b981; }
.head-adj { background: linear-gradient(135deg, #fffbeb, #fef3c7); border-bottom: 2px solid #f59e0b; }
.head-adv { background: linear-gradient(135deg, #f5f3ff, #ede9fe); border-bottom: 2px solid #8b5cf6; }
.head-prep { background: linear-gradient(135deg, #ecfeff, #cffafe); border-bottom: 2px solid #06b6d4; }
.pos-verb { border-top: 3px solid #3b82f6; }
.pos-noun { border-top: 3px solid #10b981; }
.pos-adj { border-top: 3px solid #f59e0b; }
.pos-adv { border-top: 3px solid #8b5cf6; }
.pos-prep { border-top: 3px solid #06b6d4; }

.badge-verb { background: #dbeafe; color: #1d4ed8; }
.badge-noun { background: #d1fae5; color: #047857; }
.badge-adj { background: #fef3c7; color: #b45309; }
.badge-adv { background: #ede9fe; color: #6d28d9; }
.badge-prep { background: #cffafe; color: #0e7490; }

.card-head-left { display: flex; flex-direction: column; gap: 4px; }
.card-word { font-size: 26px; font-weight: 700; }
.card-phonetic { font-size: 16px; }
.card-meaning-inline { font-size: 16px; color: var(--color-text-secondary); margin-top: 6px; padding-top: 6px; border-top: 1px dashed var(--color-border); }

.pos-badge { padding: 5px 14px; border-radius: 12px; font-size: 13px; font-weight: 600; white-space: nowrap; }

.card-meaning { padding: 12px 28px; font-size: 16px; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); }

.card-section { padding: 16px 28px; border-bottom: 1px solid var(--color-border); }
.card-section:last-of-type { border-bottom: none; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 15px; font-weight: 600; color: var(--color-text); }
.section-count { font-size: 13px; color: var(--color-text-secondary); font-weight: 400; }

.colloc-list { display: flex; flex-wrap: wrap; gap: 8px; }
.colloc-item { display: inline-flex; flex-direction: column; align-items: flex-start; gap: 2px; font-size: 14px; padding: 6px 12px; background: var(--color-bg); border-radius: 16px; cursor: pointer; transition: .15s; }
.colloc-item:hover { border-color: var(--color-primary); }
.colloc-revealed { border-color: var(--color-primary); background: var(--color-primary-light); }
.colloc-text { font-weight: 600; color: var(--color-text); white-space: nowrap; }
.colloc-trans { font-size: 13px; color: var(--color-text-secondary); white-space: nowrap; }

.prep-list { display: flex; flex-wrap: wrap; gap: 6px; }
.prep-item { display: inline-flex; flex-direction: column; align-items: flex-start; gap: 2px; font-size: 13px; padding: 4px 10px; background: var(--color-bg); border-radius: 14px; cursor: pointer; transition: .15s; }
.prep-item:hover { border-color: var(--color-primary); }
.prep-revealed { border-color: var(--color-primary); background: var(--color-primary-light); }
.prep-pattern { font-weight: 600; font-family: 'Times New Roman', serif; font-style: italic; white-space: nowrap; }
.prep-trans { font-size: 13px; color: var(--color-text-secondary); white-space: nowrap; }

.card-footer-actions { display: flex; border-top: 1px solid var(--color-border); }
.card-footer-link { flex: 1; padding: 12px; font-size: 14px; color: var(--color-primary); font-weight: 500; text-align: center; }
.card-footer-link:hover { background: var(--color-primary-light); text-decoration: none; }
.card-footer-remove, .card-footer-keypoint { flex: 1; padding: 12px; font-size: 14px; font-weight: 500; background: none; border: none; cursor: pointer; border-left: 1px solid var(--color-border); }
.card-footer-remove { color: var(--color-danger); }
.card-footer-remove:hover { background: #fef2f2; }
.card-footer-keypoint { color: var(--color-text-secondary); }
.card-footer-keypoint:hover { background: #fffbeb; }
.card-footer-keypoint.is-keypoint { color: #f59e0b; background: #fffbeb; }

.toast {
  position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
  padding: 12px 24px; border-radius: var(--radius-sm); font-size: 14px;
  cursor: pointer; z-index: 999; box-shadow: var(--shadow-lg); animation: fadeIn .2s;
}
.toast-success { background: var(--color-success); color: #fff; }
.toast-warning { background: var(--color-warning); color: #fff; }
@keyframes fadeIn { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }
</style>
