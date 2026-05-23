<template>
  <div class="search-page">
    <div class="search-hero">
      <input
        v-model="query" class="input search-input"
        placeholder="输入单词，例如 abandon..."
        @input="onSearch" @keyup.enter="onSearch"
        autofocus
      />
    </div>

    <div v-if="results.length" class="search-results">
      <div class="result-count">找到 {{ results.length }} 个结果</div>
      <div v-for="w in results" :key="w.id" class="card result-card">
        <div class="result-head">
          <router-link :to="`/word/${w.id}`" class="result-word">{{ w.word }}</router-link>
          <span class="phonetic">{{ w.phonetic_uk }}</span>
          <span class="badge badge-blue">{{ w.pos }}</span>
          <span class="badge badge-gray">{{ w.source }}</span>
          <span class="badge badge-green">难度 {{ '⭐'.repeat(w.difficulty) }}</span>
        </div>
        <div class="result-meaning">{{ w.meaning_cn }}</div>
        <div class="result-actions">
          <router-link :to="`/word/${w.id}`" class="btn btn-sm btn-primary">查看详情 →</router-link>
          <button class="btn btn-sm">⭐ 收藏</button>
          <button class="btn btn-sm">📝 笔记</button>
          <button class="btn btn-sm">🏷️ 标签</button>
        </div>
      </div>
    </div>

    <div v-else-if="query && !results.length" class="empty-state">
      <div class="icon">🔍</div>
      <p>未找到 "{{ query }}"</p>
      <p style="font-size:14px;color:var(--color-text-secondary)">试试其他拼写？</p>
    </div>

    <div v-if="searchHistory.length && !query" class="card" style="margin-top:24px">
      <div class="section-header">
        <h4>搜索历史</h4>
        <button class="btn btn-sm" @click="clearHistory">🗑️ 清除历史</button>
      </div>
      <div class="history-list">
        <div v-for="h in searchHistory" :key="h.query" class="history-item" @click="query=h.query;onSearch()">
          <span>🕐</span>
          <span class="history-query">{{ h.query }}</span>
          <span class="history-time">{{ h.searched_at }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useWordStore } from '../stores/words'

const route = useRoute()
const store = useWordStore()

const query = ref('')
const results = ref([])
const searchHistory = ref([])

onMounted(async () => {
  await store.fetchSearchHistory()
  searchHistory.value = store.searchHistory
  if (route.query.q) {
    query.value = route.query.q
    await onSearch()
  }
})

async function onSearch() {
  if (!query.value.trim()) { results.value = []; return }
  await store.search({ q: query.value })
  results.value = store.searchResults
  if (results.value.length) {
    await store.saveSearchHistory(query.value, results.value.length)
    searchHistory.value = store.searchHistory
  }
}

async function clearHistory() {
  await store.clearSearchHistory()
  searchHistory.value = []
}
</script>

<style scoped>
.search-hero { text-align: center; padding: 40px 0 20px; }
.search-input {
  max-width: 600px; margin: 0 auto; height: 48px;
  font-size: 18px; padding: 0 20px; border-radius: 24px;
}
.result-count { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.result-card { margin-bottom: 12px; }
.result-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 8px; }
.result-word { font-size: 22px; font-weight: 700; color: var(--color-primary); }
.result-word:hover { text-decoration: underline; }
.result-meaning { font-size: 15px; margin-bottom: 12px; }
.result-actions { display: flex; gap: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h4 { font-size: 15px; font-weight: 600; }
.history-list { display: flex; flex-direction: column; }
.history-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0; border-bottom: 1px solid var(--color-border);
  cursor: pointer; font-size: 14px;
}
.history-item:hover { color: var(--color-primary); }
.history-item:last-child { border-bottom: none; }
.history-query { flex: 1; font-weight: 500; }
.history-time { font-size: 13px; color: var(--color-text-secondary); }
</style>
