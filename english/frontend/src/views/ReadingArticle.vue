<template>
  <div class="reader-page">
    <div class="reader-toolbar">
      <router-link to="/reading" class="btn btn-sm">← 返回列表</router-link>
      <div class="reader-stats">查词: {{ lookupCount }} 个 | 已收藏 3 个生词</div>
      <button class="btn btn-sm btn-primary" @click="markComplete">✅ 标记读完</button>
    </div>
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
      <div class="progress-bar-fill" style="width:80%"></div>
    </div>

    <div v-if="lookupWord" class="card lookup-popup">
      <div class="lookup-header">
        <strong>{{ lookupWord }}</strong>
        <span class="phonetic">/əˈbændən/</span>
        <span style="color:var(--color-text-secondary)">vt. 放弃；遗弃；抛弃</span>
      </div>
      <div class="lookup-actions">
        <button class="btn btn-sm btn-primary">⭐ 收藏</button>
        <button class="btn btn-sm">📝 笔记</button>
        <button class="btn btn-sm">🔊</button>
        <button class="btn btn-sm" @click="lookupWord = ''">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const lookupWord = ref('')
const lookupCount = ref(5)

const article = ref({
  title: 'The Economic Logic of Climate Policy',
  source_name: 'The Economist',
  date: '2024-03',
  word_count: 1200,
  paragraphs: [
    'Climate change is one of the most pressing challenges of our time. The economic logic behind climate policy requires us to abandon the false choice between economic growth and environmental protection.',
    'Many countries have already abandoned their reliance on fossil fuels and are transitioning to renewable energy sources. This shift represents one of the most significant economic transformations in human history.',
    'Policymakers must consider the long-term costs of inaction rather than abandoning their commitments when short-term economic pressures arise.',
  ],
})

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

function lookup(word) {
  lookupWord.value = word
  lookupCount.value++
}

function markComplete() {
  alert('已标记为读完！')
}
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
