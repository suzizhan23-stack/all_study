<template>
  <div class="reading-page">
    <h2 style="margin-bottom:16px">📰 文章列表</h2>
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>
    <template v-else>
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
      <div v-if="totalPages > 1" style="display:flex;justify-content:center;align-items:center;gap:12px;margin-top:20px">
        <button class="btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span style="font-size:14px;color:var(--color-text-secondary)">{{ page }} / {{ totalPages }}</span>
        <button class="btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { articleApi } from '../api'

const loading = ref(false)
const articles = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / size.value) || 1)

async function fetchArticles() {
  loading.value = true
  try {
    const res = await articleApi.getList({ page: page.value, size: size.value })
    if (res && res.items) {
      articles.value = res.items
      total.value = res.total
    } else if (Array.isArray(res)) {
      articles.value = res
      total.value = res.length
    } else {
      articles.value = []
      total.value = 0
    }
  } catch {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  fetchArticles()
}

onMounted(fetchArticles)
</script>

<style scoped>
.article-card { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding: 20px; }
.article-info { flex: 1; margin-right: 16px; }
.article-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.article-meta { font-size: 13px; color: var(--color-text-secondary); display: flex; gap: 12px; }
</style>
