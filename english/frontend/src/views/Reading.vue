<template>
  <div class="reading-page">
    <h2 style="margin-bottom:16px">📰 文章列表</h2>
    <div v-for="a in articles" :key="a.id" class="card article-card">
      <div class="article-info">
        <h3 class="article-title">{{ a.title }}</h3>
        <div class="article-meta">
          <span>{{ a.source_name }}</span>
          <span>· {{ a.date }}</span>
          <span>· 难度 {{ '⭐'.repeat(a.difficulty) }}</span>
          <span>· {{ a.word_count }}词</span>
        </div>
        <div class="progress-bar" style="margin-top:8px">
          <div class="progress-bar-fill" :style="{ width: a.progressPct + '%' }"></div>
        </div>
        <div style="font-size:13px;color:var(--color-text-secondary);margin-top:4px">{{ a.progressPct }}%</div>
      </div>
      <router-link :to="`/reading/${a.id}`" class="btn btn-primary btn-sm">继续阅读 →</router-link>
    </div>
    <div v-if="!articles.length" class="empty-state">
      <div class="icon">📰</div>
      <p>暂无文章</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const articles = ref([
  { id: 'a1', title: 'The Economic Logic of Climate Policy', source_name: 'The Economist', date: '2024-03', difficulty: 3, word_count: 1200, progressPct: 80 },
  { id: 'a2', title: 'How Memory Works: The Science Behind Forgetting', source_name: 'Nature', date: '2024-01', difficulty: 4, word_count: 2000, progressPct: 15 },
  { id: 'a3', title: 'The Rise of Remote Work', source_name: 'BBC', date: '2024-02', difficulty: 2, word_count: 800, progressPct: 45 },
])
</script>

<style scoped>
.article-card { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding: 20px; }
.article-info { flex: 1; margin-right: 16px; }
.article-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.article-meta { font-size: 13px; color: var(--color-text-secondary); display: flex; gap: 12px; }
</style>
