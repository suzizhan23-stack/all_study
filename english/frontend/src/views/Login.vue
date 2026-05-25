<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">英语学习</h1>
      <div class="tabs">
        <button :class="['tab', { active: tab === 'login' }]" @click="tab = 'login'">登录</button>
        <button :class="['tab', { active: tab === 'register' }]" @click="tab = 'register'">注册</button>
      </div>

      <form v-if="tab === 'login'" @submit.prevent="handleLogin" class="login-form">
        <div class="field">
          <label>用户名</label>
          <input v-model="loginForm.username" class="input" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="loginForm.password" class="input" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <p v-if="loginError" class="error-msg">{{ loginError }}</p>
        <button type="submit" class="btn btn-primary btn-block" :disabled="userStore.loading">
          {{ userStore.loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else @submit.prevent="handleRegister" class="login-form">
        <div class="field">
          <label>用户名</label>
          <input v-model="registerForm.username" class="input" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model="registerForm.email" class="input" type="email" placeholder="请输入邮箱" autocomplete="email" />
        </div>
        <div class="field">
          <label>昵称</label>
          <input v-model="registerForm.nickname" class="input" placeholder="请输入昵称" autocomplete="nickname" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="registerForm.password" class="input" type="password" placeholder="请输入密码" autocomplete="new-password" />
        </div>
        <p v-if="registerError" class="error-msg">{{ registerError }}</p>
        <button type="submit" class="btn btn-primary btn-block" :disabled="userStore.loading">
          {{ userStore.loading ? '注册中...' : '注册' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

const tab = ref('login')
const loginError = ref('')
const registerError = ref('')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '', nickname: '' })

async function handleLogin() {
  loginError.value = ''
  try {
    await userStore.login(loginForm.username, loginForm.password)
    router.push('/')
  } catch (e) {
    loginError.value = e.message || '登录失败，请重试'
  }
}

async function handleRegister() {
  registerError.value = ''
  try {
    await userStore.register(registerForm)
    router.push('/')
  } catch (e) {
    registerError.value = e.message || '注册失败，请重试'
  }
}
</script>

<style scoped>
.login-page {
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; padding: 20px;
}
.login-card {
  width: 100%; max-width: 400px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 40px 32px;
  box-shadow: var(--shadow-lg);
}
.login-title {
  text-align: center; font-size: 24px; font-weight: 700;
  margin-bottom: 24px;
}
.tabs {
  display: flex; border-bottom: 1px solid var(--color-border);
  margin-bottom: 24px;
}
.tab {
  flex: 1; padding: 10px; text-align: center;
  background: none; border: none; cursor: pointer;
  font-size: 15px; font-weight: 500;
  color: var(--color-text-secondary);
  border-bottom: 2px solid transparent;
  transition: .15s;
}
.tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}
.tab:hover { color: var(--color-text); }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 14px; font-weight: 500; }
.error-msg { color: var(--color-danger); font-size: 13px; margin: -4px 0; }
.btn-block { width: 100%; justify-content: center; padding: 10px; font-size: 15px; }
.btn-block:disabled { opacity: .6; cursor: not-allowed; }
@media (max-width: 480px) {
  .login-card { padding: 24px 20px; }
  .login-title { font-size: 20px; }
}
</style>
