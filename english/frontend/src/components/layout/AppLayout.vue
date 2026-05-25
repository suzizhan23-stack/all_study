<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">📚 英语</router-link>
        <div class="header-search">
          <input v-model="searchQuery" class="input" placeholder="搜索单词..." @keyup.enter="doSearch" />
        </div>
        <button class="hamburger" @click="menuOpen = !menuOpen" :class="{ 'hamburger-open': menuOpen }">
          <span></span><span></span><span></span>
        </button>
        <nav class="header-nav" :class="{ 'nav-open': menuOpen }">
          <router-link to="/word-books" class="nav-link" @click="menuOpen = false">单词本</router-link>
          <router-link to="/learn" class="nav-link" @click="menuOpen = false">学习</router-link>
          <router-link to="/review" class="nav-link" @click="menuOpen = false">复习</router-link>
          <router-link to="/reading" class="nav-link" @click="menuOpen = false">阅读</router-link>
          <router-link to="/favorites" class="nav-link" @click="menuOpen = false">收藏</router-link>
          <router-link to="/wrong-answers" class="nav-link" @click="menuOpen = false">错题</router-link>
          <router-link to="/leaderboard" class="nav-link" @click="menuOpen = false">排行</router-link>
          <template v-if="userStore.isLoggedIn && userStore.user">
            <router-link to="/profile" class="nav-link nav-user" @click="menuOpen = false">
              <img v-if="userStore.user.avatar" :src="userStore.user.avatar" class="user-avatar" />
              {{ userStore.user.nickname || userStore.user.username }}
            </router-link>
            <button class="nav-link" @click="logoutClick">退出</button>
          </template>
          <router-link v-else to="/login" class="nav-link nav-user" @click="menuOpen = false">登录</router-link>
        </nav>
      </div>
    </header>
    <div v-if="menuOpen" class="nav-overlay" @click="handleOverlayClick"></div>
    <main class="main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const searchQuery = ref('')
const menuOpen = ref(false)

onMounted(() => {
  userStore.fetchProfile()
})

function go(path) {
  menuOpen.value = false
  router.push(path)
}

function handleOverlayClick() {
  menuOpen.value = false
}

function logoutClick() {
  menuOpen.value = false
  setTimeout(() => logout(), 80)
}

function doSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value.trim() } })
  }
}

function logout() {
  userStore.logout()
  router.push('/login')
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
.hamburger { display: none; flex-direction: column; gap: 4px; background: none; border: none; cursor: pointer; padding: 4px; }
.hamburger span { display: block; width: 22px; height: 2px; background: var(--color-text); border-radius: 2px; transition: .2s; }
.hamburger-open span:nth-child(1) { transform: translateY(6px) rotate(45deg); }
.hamburger-open span:nth-child(2) { opacity: 0; }
.hamburger-open span:nth-child(3) { transform: translateY(-6px) rotate(-45deg); }
.nav-overlay { display: none; }
@media (max-width: 768px) {
  .hamburger { display: flex; }
  .header-nav {
    position: fixed; top: 60px; right: 0; bottom: 0;
    width: 260px; background: var(--color-surface); flex-direction: column;
    padding: 12px; gap: 2px; border-left: 1px solid var(--color-border);
    transform: translateX(100%); transition: transform .2s; z-index: 200;
  }
  .header-nav.nav-open { transform: translateX(0); }
  .nav-link { width: 100%; padding: 12px 14px; font-size: 15px; }
  .header-search { max-width: 180px; }
  .nav-overlay { display: block; position: fixed; inset: 0; background: rgba(0,0,0,.3); z-index: 99; }
  .main { padding: 16px 12px; }
}
</style>
