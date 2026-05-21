<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">📚 英语学习</router-link>
        <div class="header-search">
          <input v-model="searchQuery" class="input" placeholder="搜索单词..." @keyup.enter="doSearch" />
        </div>
        <nav class="header-nav">
          <router-link to="/word-books" class="nav-link">单词本</router-link>
          <router-link to="/learn" class="nav-link">学习</router-link>
          <router-link to="/review" class="nav-link">复习</router-link>
          <router-link to="/reading" class="nav-link">阅读</router-link>
          <router-link to="/favorites" class="nav-link">收藏</router-link>
          <router-link to="/wrong-answers" class="nav-link">错题</router-link>
          <router-link to="/leaderboard" class="nav-link">排行</router-link>
          <router-link to="/profile" class="nav-link nav-user">👤 演示用户</router-link>
        </nav>
      </div>
    </header>
    <main class="main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')

function doSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value.trim() } })
  }
}
</script>

<style scoped>
.layout { min-height: 100vh; display: flex; flex-direction: column; }
.header {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky; top: 0; z-index: 100;
}
.header-inner {
  max-width: 1200px; margin: 0 auto; padding: 0 20px;
  height: 60px; display: flex; align-items: center; gap: 24px;
}
.logo { font-size: 18px; font-weight: 700; color: var(--color-primary); white-space: nowrap; }
.logo:hover { text-decoration: none; }
.header-search { flex: 1; max-width: 320px; }
.header-search .input { height: 36px; font-size: 14px; }
.header-nav { display: flex; align-items: center; gap: 4px; }
.nav-link {
  padding: 8px 14px; border-radius: var(--radius-sm);
  font-size: 14px; color: var(--color-text-secondary); transition: .15s;
}
.nav-link:hover { background: var(--color-bg); color: var(--color-text); text-decoration: none; }
.nav-link.router-link-active { color: var(--color-primary); font-weight: 600; }
.nav-user { font-weight: 500; color: var(--color-text); }
.main { flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 24px 20px; }
</style>
