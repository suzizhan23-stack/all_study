<template>
  <div class="reader-page">
    <div class="reader-toolbar">
      <router-link to="/reading" class="btn btn-sm">← 返回列表</router-link>
      <div class="reader-stats">查词: {{ lookupCount }} 个</div>
      <button class="btn btn-sm btn-primary" @click="markComplete">✅ 标记读完</button>
    </div>
    <div v-if="loading" style="text-align:center;padding:60px;color:var(--color-text-secondary)">加载中...</div>
    <template v-else>
      <h1 class="reader-title">{{ article.title }}</h1>
      <div class="reader-meta">{{ article.source_name }} · {{ article.date }} · {{ article.word_count }} 词</div>
      <div class="reader-content">
        <p v-for="(p, i) in article.paragraphs" :key="i">
          <template v-for="(seg, j) in tokenize(p)" :key="j">
            <span v-if="seg.type === 'word'" class="reader-word" @click="lookup(seg.text)" :title="seg.text">
              {{ seg.text }}
            </span>
            <span v-else>{{ seg.text }}</span>
          </template>
        </p>
      </div>
      <div class="progress-bar reader-progress">
        <div class="progress-bar-fill" :style="{ width: scrollPercent + '%' }"></div>
      </div>
    </template>

    <div v-if="lookupWord" class="card lookup-popup">
      <div class="lookup-header">
        <strong>{{ lookupWord }}</strong>
        <span v-if="lookupData?.phonetic" class="phonetic">{{ lookupData.phonetic }}</span>
        <span v-if="lookupData?.meaning" style="color:var(--color-text-secondary)">{{ lookupData.meaning }}</span>
      </div>
      <div class="lookup-actions">
        <button class="btn btn-sm btn-primary">⭐ 收藏</button>
        <button class="btn btn-sm">📝 笔记</button>
        <button class="btn btn-sm">🔊</button>
        <button class="btn btn-sm" @click="lookupWord = ''; lookupData = null">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi } from '../api'

const route = useRoute()
const articleId = computed(() => route.params.id)
const loading = ref(true)
const lookupWord = ref('')
const lookupData = ref(null)
const lookupCount = ref(0)
const scrollPercent = ref(0)

const article = ref({
  title: '',
  source_name: '',
  date: '',
  word_count: 0,
  paragraphs: [],
})

let scrollTimer = null
const readingTimeSec = ref(0)
let readingTimer = null

function tokenize(text) {
  const parts = []
  const re = /(\b[a-zA-Z]{4,}\b)/g
  let last = 0, m
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) parts.push({ type: 'text', text: text.slice(last, m.index) })
    parts.push({ type: 'word', text: m[1] })
    last = m.index + m[0].length
  }
  if (last < text.length) parts.push({ type: 'text', text: text.slice(last) })
  return parts
}

async function lookup(word) {
  lookupWord.value = word
  lookupCount.value++
  try {
    lookupData.value = await articleApi.lookup(articleId.value, word)
  } catch {
    lookupData.value = null
  }
}

async function saveProgress() {
  try {
    await articleApi.updateProgress(articleId.value, {
      scrollPosition: window.scrollY,
      readingTimeSec: readingTimeSec.value,
    })
  } catch {}
}

function onScroll() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  scrollPercent.value = docHeight > 0 ? Math.min(100, Math.round((scrollTop / docHeight) * 100)) : 0
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(saveProgress, 1000)
}

async function markComplete() {
  try {
    await articleApi.complete(articleId.value)
    alert('已标记为读完！')
  } catch {}
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await articleApi.getDetail(articleId.value)
    if (data) Object.assign(article.value, data)
    window.addEventListener('scroll', onScroll)
    readingTimer = setInterval(() => { readingTimeSec.value++ }, 1000)
  } catch {
    article.value.paragraphs = []
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  clearTimeout(scrollTimer)
  clearInterval(readingTimer)
})
</script>

<style scoped>
.reader-toolbar { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--color-border); margin-bottom: 20px; }
.reader-stats { flex: 1; font-size: 14px; color: var(--color-text-secondary); }
.reader-title { font-size: 24px; font-weight: 700; margin-bottom: 8px; }
.reader-meta { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 24px; }
.reader-content { font-size: 16px; line-height: 1.9; }
.reader-content p { margin-bottom: 16px; }
.reader-word { color: var(--color-primary); cursor: pointer; border-bottom: 1px dashed var(--color-primary); }
.reader-word:hover { background: var(--color-primary-light); }
.reader-progress { margin-top: 24px; }
.lookup-popup { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); width: 500px; z-index: 200; padding: 16px; }
.lookup-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.lookup-actions { display: flex; gap: 8px; }
</style>
