<template>
  <div class="plans-page">
    <h2 style="margin-bottom:16px">📋 学习计划</h2>

    <!-- 进行中的计划 -->
    <div v-if="activePlan" class="card active-plan" style="margin-bottom:20px">
      <h3>进行中</h3>
      <div class="plan-header">
        <div>
          <div class="plan-name">{{ activePlan.name }}</div>
          <div class="plan-sub">
            第 {{ activePlan.current_day }} 天 / 共 {{ activePlan.duration }} 天
            <span class="plan-meta" style="margin-left:8px">· {{ activePlan.bookName }} · {{ strategyName(activePlan.strategyId) }}</span>
          </div>
          <div class="progress-bar" style="margin-top:8px;max-width:400px">
            <div class="progress-bar-fill" :style="{ width: activePlan.pct + '%' }"></div>
          </div>
          <div class="plan-today" style="margin-top:8px">
            今日任务: {{ activePlan.daily }} 词 | 已完成 {{ activePlan.done }} 词
          </div>
        </div>
        <router-link to="/review" class="btn btn-primary">继续学习 →</router-link>
      </div>
    </div>

    <!-- 创建新计划 -->
    <h3 style="margin-bottom:12px">创建新计划</h3>
    <div class="card plan-creator">
      <!-- 选择单词本 -->
      <div class="creator-row">
        <label>单词本：</label>
        <div class="book-options">
          <div
            v-for="b in store.books"
            :key="b.id"
            class="chip"
            :class="{ 'chip-active': newPlanBook === b.id }"
            @click="newPlanBook = b.id"
          >{{ b.name }} <span class="chip-count">{{ b.word_count }}词</span></div>
        </div>
      </div>

      <!-- 选择策略 -->
      <div class="creator-row">
        <label>策略：</label>
        <div class="strategy-options">
          <div
            v-for="s in store.strategies"
            :key="s.id"
            class="strategy-chip"
            :class="{ 'strategy-active': newPlanStrategy === s.id }"
            @click="newPlanStrategy = s.id"
          >
            <div class="strategy-chip-name">{{ s.name }}</div>
            <div class="strategy-chip-desc">{{ s.description }}</div>
          </div>
        </div>
      </div>

      <!-- 每日词数 -->
      <div class="creator-row" style="align-items:center">
        <label>每日词数：</label>
        <input v-model.number="newPlanDaily" class="input" type="number" min="5" max="100" style="max-width:100px" />
        <span style="font-size:14px;color:var(--color-text-secondary);margin-left:8px">词 / 天</span>
        <span style="font-size:14px;color:var(--color-text-secondary);margin-left:12px">周期约 {{ estimatedDays }} 天</span>
      </div>

      <div class="creator-actions">
        <button class="btn btn-success btn-lg" @click="createPlan" :disabled="!newPlanBook || !newPlanStrategy">
          🚀 开始学习
        </button>
        <span v-if="!newPlanBook || !newPlanStrategy" class="hint-text">请先选择单词本和策略</span>
      </div>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>

    <!-- 可选预置计划（历史保留，改为推荐） -->
    <h3 v-if="templates.length" style="margin-bottom:12px;margin-top:24px">推荐计划</h3>
    <div v-if="templates.length" class="grid-3">
      <div v-for="p in templates" :key="p.id" class="card plan-card">
        <div class="plan-card-name">{{ p.name }}</div>
        <div class="plan-card-detail">{{ p.duration }} 天 · 每日 {{ p.dailyCount }} 词</div>
        <div class="plan-card-target" v-if="p.target">{{ p.target }}</div>
        <p class="plan-card-desc">{{ p.description }}</p>
        <button class="btn btn-primary btn-sm" style="margin-top:12px" @click="applyRecommended(p)">使用此配置</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWordBookStore } from '../stores/wordBooks'
import { planApi } from '../api'

const router = useRouter()
const store = useWordBookStore()

const activePlan = ref(null)
const newPlanBook = ref('')
const newPlanStrategy = ref('')
const newPlanDaily = ref(10)
const templates = ref([])
const loading = ref(false)

const currentBook = computed(() => store.books.find(b => b.id === newPlanBook.value))
const estimatedDays = computed(() => {
  if (!currentBook.value) return 0
  return Math.ceil(currentBook.value.word_count / newPlanDaily.value)
})

async function createPlan() {
  const book = store.books.find(b => b.id === newPlanBook.value)
  const strategy = store.strategies.find(s => s.id === newPlanStrategy.value)
  if (!book || !strategy) return

  loading.value = true
  try {
    const plan = await planApi.generate({
      wordBookId: newPlanBook.value,
      strategyId: newPlanStrategy.value,
      count: newPlanDaily.value,
    })
    activePlan.value = plan
    router.push('/review')
  } finally {
    loading.value = false
  }
}

function strategyName(strategyId) {
  const s = store.strategies.find(s => s.id === strategyId)
  return s ? s.name : ''
}

function applyRecommended(p) {
  newPlanBook.value = p.wordBookId
  newPlanStrategy.value = p.strategyId
  newPlanDaily.value = p.dailyCount
}

onMounted(async () => {
  loading.value = true
  try {
    const [active, tmpl] = await Promise.all([
      planApi.getActive(),
      planApi.getTemplates(),
      store.fetchBooks(),
      store.fetchStrategies(),
    ])
    activePlan.value = active
    templates.value = tmpl || []
    if (store.books.length && templates.value.length) {
      newPlanBook.value = store.books[0].id
      newPlanStrategy.value = store.strategies[0]?.id || ''
    }
  } catch {
    templates.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.active-plan { border: 2px solid var(--color-primary); }
.plan-header { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 12px; }
.plan-name { font-size: 18px; font-weight: 600; }
.plan-sub { font-size: 14px; color: var(--color-text-secondary); }
.plan-meta { font-size: 13px; }
.plan-today { font-size: 14px; }
.plan-card { padding: 20px; }
.plan-card-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.plan-card-detail { font-size: 14px; color: var(--color-text-secondary); }
.plan-card-target { display: inline-block; font-size: 12px; padding: 2px 8px; background: var(--color-primary-light); color: var(--color-primary); border-radius: 4px; margin-top: 4px; }
.plan-card-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 6px; }

.plan-creator { display: flex; flex-direction: column; gap: 16px; }
.creator-row { display: flex; gap: 12px; align-items: flex-start; }
.creator-row > label { min-width: 80px; font-size: 14px; font-weight: 500; padding-top: 6px; }
.creator-actions { display: flex; align-items: center; gap: 12px; padding-top: 8px; padding-left: 92px; }
.hint-text { font-size: 13px; color: var(--color-text-secondary); }

.book-options { display: flex; gap: 8px; flex-wrap: wrap; }
.chip { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border: 1px solid var(--color-border); border-radius: 20px; cursor: pointer; font-size: 14px; transition: .15s; }
.chip:hover { border-color: var(--color-primary); }
.chip-active { border-color: var(--color-primary); background: var(--color-primary-light); }
.chip-count { font-size: 12px; color: var(--color-text-secondary); }

.strategy-options { display: flex; flex-wrap: wrap; gap: 8px; }
.strategy-chip { padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); cursor: pointer; transition: .15s; min-width: 140px; }
.strategy-chip:hover { border-color: var(--color-primary); }
.strategy-active { border-color: var(--color-primary); background: var(--color-primary-light); }
.strategy-chip-name { font-size: 14px; font-weight: 600; }
.strategy-chip-desc { font-size: 12px; color: var(--color-text-secondary); }

.btn-lg { padding: 12px 32px; font-size: 16px; }
</style>
