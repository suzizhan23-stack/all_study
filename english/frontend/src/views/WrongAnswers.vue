<template>
  <div class="wrong-page">
    <h2 style="margin-bottom:16px">❌ 错题本</h2>

    <div class="card" style="margin-bottom:16px">
      <div class="wrong-stats">
        <div class="wrong-stat">
          <span class="wrong-stat-value">{{ recent7Count }}</span>
          <span class="wrong-stat-label">近7天错题</span>
        </div>
        <div class="wrong-stat">
          <span class="wrong-stat-value">{{ topWrongWord }}</span>
          <span class="wrong-stat-label">最高错词</span>
        </div>
        <div class="wrong-stat">
          <span class="wrong-stat-value">{{ weakType }}</span>
          <span class="wrong-stat-label">薄弱题型</span>
        </div>
      </div>
    </div>

    <div class="wrong-type-tabs" style="margin-bottom:12px">
      <button class="btn" :class="quizType === '' ? 'btn-primary' : ''" @click="setType('')">全部</button>
      <button class="btn" :class="quizType === 'spelling' ? 'btn-primary' : ''" @click="setType('spelling')">拼写</button>
      <button class="btn" :class="quizType === 'listening' ? 'btn-primary' : ''" @click="setType('listening')">听力</button>
      <button class="btn" :class="quizType === 'meaning' ? 'btn-primary' : ''" @click="setType('meaning')">释义</button>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>
    <template v-else>
      <div v-if="!wrongWords.length" class="empty-state">
        <div class="icon">✅</div>
        <p>暂无错题，继续保持！</p>
      </div>
      <div v-for="w in wrongWords" :key="w.word_id" class="card wrong-item">
        <div class="wrong-head">
          <strong class="wrong-word">{{ w.word }}</strong>
          <span class="badge badge-red">错 {{ w.count }} 次</span>
          <router-link :to="`/review`" class="btn btn-sm btn-primary">巩固复习</router-link>
        </div>
        <div v-for="log in w.logs" :key="log.time" class="wrong-log">
          <span class="badge" :class="log.type === '拼写' ? 'badge-gray' : 'badge-blue'">{{ log.type }}</span>
          <span v-if="log.answer" class="wrong-answer">"{{ log.answer }}"</span>
          <span class="wrong-time">{{ log.time }}</span>
        </div>
      </div>
      <div v-if="totalPages > 1" style="display:flex;justify-content:center;align-items:center;gap:12px;margin-top:20px">
        <button class="btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span style="font-size:14px;color:var(--color-text-secondary)">{{ page }} / {{ totalPages }}</span>
        <button class="btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
      <div class="card" style="margin-top:16px;text-align:center;padding:20px">
        <router-link to="/review" class="btn btn-primary">一键复习全部错题</router-link>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { wrongWordApi } from '../api'

const loading = ref(false)
const wrongWords = ref([])
const quizType = ref('')
const days = ref(30)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / size.value) || 1)

const recent7Count = computed(() => {
  const cutoff = new Date()
  cutoff.setDate(cutoff.getDate() - 7)
  return wrongWords.value.reduce((sum, w) => {
    return sum + (w.logs || []).filter(l => new Date(l.time) >= cutoff).length
  }, 0)
})

const topWrongWord = computed(() => {
  if (!wrongWords.value.length) return '-'
  return wrongWords.value.reduce((a, b) => (a.count > b.count ? a : b)).word
})

const weakType = computed(() => {
  const typeCount = {}
  wrongWords.value.forEach(w => (w.logs || []).forEach(l => {
    typeCount[l.type] = (typeCount[l.type] || 0) + 1
  }))
  let maxType = '-', maxCount = 0
  for (const [t, c] of Object.entries(typeCount)) {
    if (c > maxCount) { maxCount = c; maxType = t }
  }
  return maxType
})

async function fetchWrongWords() {
  loading.value = true
  try {
    const params = { quizType: quizType.value || undefined, days: days.value, page: page.value, size: size.value }
    const res = await wrongWordApi.getList(params)
    if (res && res.items) {
      wrongWords.value = res.items
      total.value = res.total
    } else if (Array.isArray(res)) {
      wrongWords.value = res
      total.value = res.length
    } else {
      wrongWords.value = []
      total.value = 0
    }
  } catch {
    wrongWords.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function setType(type) {
  quizType.value = type
  page.value = 1
  fetchWrongWords()
}

function changePage(p) {
  page.value = p
  fetchWrongWords()
}

onMounted(fetchWrongWords)
</script>

<style scoped>
.wrong-stats { display: flex; gap: 24px; justify-content: center; }
.wrong-stat { text-align: center; }
.wrong-stat-value { display: block; font-size: 20px; font-weight: 700; }
.wrong-stat-label { font-size: 13px; color: var(--color-text-secondary); }
.wrong-type-tabs { display: flex; gap: 4px; }
.wrong-item { margin-bottom: 12px; }
.wrong-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.wrong-word { font-size: 16px; }
.wrong-log { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 14px; }
.wrong-answer { color: var(--color-danger); }
.wrong-time { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }
</style>
