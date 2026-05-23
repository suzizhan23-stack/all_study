<template>
  <div class="admin-page">
    <h2 style="margin-bottom:16px">⚙️ 管理后台</h2>
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <button v-for="tab in tabs" :key="tab.key" class="btn admin-tab" :class="{ 'btn-primary': activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
      </aside>
      <main class="admin-content">

        <div v-if="loading" class="loading">加载中...</div>

        <div v-if="!loading && activeTab === 'overview'" class="card">
          <h3>平台概览</h3>
          <div class="overview-grid">
            <div class="overview-item"><strong>{{ overview.totalUsers || 0 }}</strong> 总用户</div>
            <div class="overview-item"><strong>{{ overview.totalWords || 0 }}</strong> 总词汇</div>
            <div class="overview-item"><strong>{{ overview.activeToday || 0 }}</strong> 今日活跃</div>
          </div>
        </div>

        <div v-if="!loading && activeTab === 'users'" class="card">
          <div class="admin-table-header">
            <h3>用户管理</h3>
            <input v-model="userSearch" class="input" placeholder="搜索用户..." style="max-width:200px" />
          </div>
          <table class="table">
            <thead>
              <tr><th>用户名</th><th>角色</th><th>注册时间</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="u in filteredUsers" :key="u.id">
                <td>{{ u.username }}</td>
                <td><span class="badge" :class="u.role === 'admin' ? 'badge-red' : 'badge-blue'">{{ u.role }}</span></td>
                <td>{{ u.createdAt || u.created_at || '-' }}</td>
                <td><span class="badge" :class="u.isActive !== false ? 'badge-green' : 'badge-gray'">{{ u.isActive !== false ? '激活' : '禁用' }}</span></td>
                <td><button class="btn btn-sm" @click="toggleUserStatus(u)">{{ u.isActive !== false ? '禁用' : '启用' }}</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!loading && activeTab === 'words'" class="card">
          <div class="admin-table-header">
            <h3>词库管理</h3>
            <div>
              <button class="btn btn-sm btn-primary" @click="showImport = !showImport">批量导入</button>
            </div>
          </div>
          <div v-if="showImport" style="margin:12px 0;padding:12px;background:var(--color-bg);border-radius:var(--radius-sm)">
            <textarea v-model="importText" class="input" rows="4" placeholder="每行一个单词: word,pos,meaning_cn" style="font-family:monospace"></textarea>
            <button class="btn btn-sm btn-primary" style="margin-top:8px" @click="handleImport">导入</button>
          </div>
          <table class="table">
            <thead><tr><th>单词</th><th>词性</th><th>释义</th><th>来源</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="w in wordList" :key="w.id">
                <td><strong>{{ w.word }}</strong></td>
                <td>{{ w.pos }}</td>
                <td>{{ w.meaningCn || w.meaning }}</td>
                <td><span class="badge badge-gray">{{ w.source }}</span></td>
                <td><button class="btn btn-sm btn-danger" @click="deleteWord(w)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!loading && activeTab === 'feedback'" class="card">
          <h3>用户反馈</h3>
          <table class="table">
            <thead><tr><th>用户</th><th>实体</th><th>评分</th><th>反馈</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="f in feedbacks" :key="f.id">
                <td>{{ f.user || f.userId }}</td>
                <td>{{ f.entity || f.entityType }}</td>
                <td>{{ '⭐'.repeat(f.rating || 1) }}</td>
                <td>{{ f.feedback || f.content }}</td>
                <td>{{ f.createdAt || f.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>

      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../api'

const activeTab = ref('overview')
const userSearch = ref('')
const showImport = ref(false)
const loading = ref(false)

const tabs = [
  { key: 'overview', label: '📊 总览' },
  { key: 'users', label: '👥 用户' },
  { key: 'words', label: '📖 词库' },
  { key: 'feedback', label: '💬 反馈' },
]

const overview = ref({ users: 0, words: 0, active: 0 })
const users = ref([])
const wordList = ref([])
const feedbacks = ref([])
const userPage = ref(1)
const wordPage = ref(1)
const feedbackPage = ref(1)

const filteredUsers = computed(() =>
  users.value.filter(u => (u.username || '').includes(userSearch.value))
)

async function fetchOverview() {
  try { overview.value = await adminApi.getOverview() } catch {}
}

async function fetchUsers() {
  try {
    const res = await adminApi.getUsers({ page: userPage.value, size: 20 })
    users.value = res.list || []
  } catch {}
}

async function fetchWords() {
  try {
    const res = await adminApi.getWords({ page: wordPage.value, size: 20 })
    wordList.value = res.list || []
  } catch {}
}

async function fetchFeedback() {
  try {
    const res = await adminApi.getFeedback({ page: feedbackPage.value, size: 20 })
    feedbacks.value = res.list || []
  } catch {}
}

async function toggleUserStatus(u) {
  try {
    await adminApi.toggleUserStatus(u.id, !u.isActive)
    u.isActive = !u.isActive
  } catch {}
}

const importText = ref('')
async function handleImport() {
  try {
    const lines = importText.value.trim().split('\n').filter(Boolean)
    const words = lines.map(line => {
      const [word, pos, meaning_cn] = line.split(',').map(s => s.trim())
      return { word, pos, meaning_cn }
    })
    await adminApi.batchImport({ words, wordBookId: '' })
    importText.value = ''
    showImport.value = false
    fetchWords()
  } catch {}
}

async function deleteWord(w) {
  try {
    await adminApi.deleteWord(w.id || w.uuid)
    wordList.value = wordList.value.filter(x => x.id !== w.id)
  } catch {}
}

onMounted(() => {
  fetchOverview()
  fetchUsers()
  fetchWords()
  fetchFeedback()
})
</script>

<style scoped>
.admin-layout { display: flex; gap: 20px; }
.admin-sidebar { display: flex; flex-direction: column; gap: 4px; min-width: 120px; }
.admin-tab { text-align: left; justify-content: flex-start; }
.admin-content { flex: 1; }
.overview-grid { display: flex; gap: 20px; margin-top: 12px; }
.overview-item { flex: 1; text-align: center; padding: 20px; background: var(--color-bg); border-radius: var(--radius-sm); }
.overview-item strong { display: block; font-size: 28px; }
.admin-table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.admin-table-header h3 { margin: 0; }
</style>
