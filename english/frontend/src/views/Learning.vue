<template>
  <div class="learning-page">
    <!-- 计划日期选择 -->
    <div class="plan-selector card">
      <div class="plan-selector-row">
        <h3 style="margin:0">📋 学习计划</h3>
        <div class="plan-date-nav">
          <button class="btn btn-sm" :disabled="!prevDate" @click="goDate(-1)">‹ 前一天</button>
          <select v-model="selectedDate" class="input plan-date-select" @change="onDateChange">
            <option v-for="d in planStore.dailyDates" :key="d" :value="d">
              {{ formatLabel(d) }}
            </option>
          </select>
          <button class="btn btn-sm" :disabled="!nextDate" @click="goDate(1)">后一天 ›</button>
        </div>
      </div>
      <div class="plan-summary">
        <span v-if="isToday" class="badge badge-green">今天</span>
        <span class="plan-count">{{ planStore.dailyWords.length }} 个单词</span>
        <router-link to="/word-books" class="btn btn-sm btn-primary" style="margin-left:auto">从单词本添加</router-link>
      </div>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>

    <!-- 计划为空 -->
    <div v-if="planStore.dailyWords.length === 0" class="card empty-state" style="padding:60px;text-align:center">
      <p style="font-size:16px;margin-bottom:12px">
        {{ isToday ? '今天还没有学习计划' : '该日期没有学习计划' }}
      </p>
      <p style="font-size:14px;color:var(--color-text-secondary);margin-bottom:16px">
        {{ isToday ? '去单词本中选择单词加入今天的计划吧' : '选择一个有计划的日期，或回到今天' }}
      </p>
      <router-link v-if="isToday" to="/word-books" class="btn btn-primary">去单词本添加 →</router-link>
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
    </div>

    <!-- 单词卡片列表 -->
    <div v-if="planStore.dailyWords.length > 0" class="learn-grid">
      <div
        v-for="item in filteredWords" :key="item.id"
        class="card learn-card"
        :class="'pos-' + item.posKey"
      >
        <div class="card-head" :class="'head-' + item.posKey">
          <div class="card-head-left">
            <div class="card-word">{{ item.word }}</div>
            <div class="card-phonetic phonetic">{{ item.phonetic }}</div>
          </div>
          <span class="pos-badge" :class="'badge-' + item.posKey">{{ item.posLabel }}</span>
        </div>

        <div v-if="item.collocations && item.collocations.length" class="card-section">
          <div class="section-title">
            <span>常用搭配</span>
            <span class="section-count">{{ item.collocations.length }} 个</span>
          </div>
          <div class="colloc-list">
            <div v-for="c in item.collocations" :key="c.text" class="colloc-item">
              <span class="colloc-text">{{ c.text }}</span>
              <span class="colloc-freq badge badge-gray">{{ c.frequency }}</span>
            </div>
          </div>
        </div>

        <div v-if="item.preps && item.preps.length" class="card-section">
          <div class="section-title">
            <span>介词短语</span>
            <span class="section-count">{{ item.preps.length }} 个</span>
          </div>
          <div class="prep-list">
            <div v-for="p in item.preps" :key="p.pattern" class="prep-item">
              <span class="prep-pattern">{{ p.pattern }}</span>
              <span class="prep-tag badge badge-blue">{{ p.preposition }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer-actions">
          <router-link :to="`/word/${item.id}`" class="card-footer-link">查看完整详情 →</router-link>
          <button class="card-footer-remove" @click="removeFromPlan(item.id)">🗑️ 移出计划</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDailyPlanStore } from '../stores/dailyPlan'

const planStore = useDailyPlanStore()

const activePos = ref('')
const selectedDate = ref('')
const loading = ref(false)

const posLabels = { verb: '动词', noun: '名词', adj: '形容词', adv: '副词', prep: '介词' }

const posCategories = [
  { key: 'verb', label: '动词', color: '#3b82f6' },
  { key: 'noun', label: '名词', color: '#10b981' },
  { key: 'adj', label: '形容词', color: '#f59e0b' },
  { key: 'adv', label: '副词', color: '#8b5cf6' },
  { key: 'prep', label: '介词', color: '#06b6d4' },
]

const today = computed(() => new Date().toISOString().slice(0, 10))

const isToday = computed(() => selectedDate.value === today.value)

const prevDate = computed(() => {
  const idx = planStore.dailyDates.indexOf(selectedDate.value)
  return idx < planStore.dailyDates.length - 1 ? planStore.dailyDates[idx + 1] : null
})

const nextDate = computed(() => {
  const idx = planStore.dailyDates.indexOf(selectedDate.value)
  return idx > 0 ? planStore.dailyDates[idx - 1] : null
})

const dailyWordsMapped = computed(() => {
  return (planStore.dailyWords || []).map(w => ({
    ...w,
    phonetic: w.phoneticUk || w.phonetic || '',
    posKey: w.pos,
    posLabel: posLabels[w.pos] || w.posLabel || w.pos || '',
    collocations: w.collocations || [],
    preps: w.preps || [],
  }))
})

const filteredWords = computed(() => {
  if (!activePos.value) return dailyWordsMapped.value
  return dailyWordsMapped.value.filter(w => w.posKey === activePos.value)
})

async function fetchPlanData(date) {
  loading.value = true
  try {
    await Promise.all([
      planStore.fetchDailyDates(30),
      planStore.fetchDailyWords(date),
    ])
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  selectedDate.value = today.value
  await fetchPlanData(selectedDate.value)
})

function formatLabel(dateStr) {
  const d = new Date(dateStr)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const label = `${dateStr} 周${weekdays[d.getDay()]}`
  if (dateStr === today.value) return label + ' (今天)'
  return label
}

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
  fetchPlanData(today.value)
}

function removeFromPlan(wordId) {
  planStore.deleteEntry(wordId)
}
</script>

<style scoped>
.plan-selector { margin-bottom: 20px; padding: 16px 20px; }
.plan-selector-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.plan-date-nav { display: flex; align-items: center; gap: 8px; }
.plan-date-select { min-width: 200px; text-align: center; }
.plan-summary { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
.plan-count { font-size: 14px; color: var(--color-text-secondary); }

.learn-filter-bar { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; margin-bottom: 20px; }
.filter-count { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }

.learn-grid {
  columns: 280px 3;
  column-gap: 20px;
}
.learn-card {
  break-inside: avoid;
  margin-bottom: 20px;
}

.learn-card { padding: 0; overflow: hidden; transition: .2s; }
.learn-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); }

.card-head { padding: 20px 24px 14px; display: flex; justify-content: space-between; align-items: flex-start; }
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
.card-word { font-size: 22px; font-weight: 700; }
.card-phonetic { font-size: 14px; }

.pos-badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; white-space: nowrap; }

.card-meaning { padding: 10px 24px; font-size: 15px; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); }

.card-section { padding: 12px 24px; border-bottom: 1px solid var(--color-border); }
.card-section:last-of-type { border-bottom: none; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: var(--color-text); }
.section-count { font-size: 12px; color: var(--color-text-secondary); font-weight: 400; }

.colloc-list { display: flex; flex-wrap: wrap; gap: 6px; }
.colloc-item { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; padding: 4px 10px; background: var(--color-bg); border-radius: 14px; white-space: nowrap; }
.colloc-text { font-weight: 600; color: var(--color-text); }

.colloc-freq { font-size: 11px; padding: 1px 8px; }

.prep-list { display: flex; flex-wrap: wrap; gap: 6px; }
.prep-item { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; padding: 4px 10px; background: var(--color-bg); border-radius: 14px; white-space: nowrap; }
.prep-pattern { font-weight: 600; font-family: 'Times New Roman', serif; font-style: italic; }

.prep-tag { font-size: 11px; padding: 1px 8px; }

.card-footer-actions { display: flex; border-top: 1px solid var(--color-border); }
.card-footer-link { flex: 1; padding: 10px; font-size: 13px; color: var(--color-primary); font-weight: 500; text-align: center; }
.card-footer-link:hover { background: var(--color-primary-light); text-decoration: none; }
.card-footer-remove { flex: 1; padding: 10px; font-size: 13px; color: var(--color-danger); font-weight: 500; background: none; border: none; cursor: pointer; border-left: 1px solid var(--color-border); }
.card-footer-remove:hover { background: #fef2f2; }
</style>
