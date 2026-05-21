<template>
  <div class="admin-page">
    <h2 style="margin-bottom:16px">⚙️ 管理后台</h2>
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <button v-for="tab in tabs" :key="tab.key" class="btn admin-tab" :class="{ 'btn-primary': activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
      </aside>
      <main class="admin-content">

        <div v-if="activeTab === 'overview'" class="card">
          <h3>平台概览</h3>
          <div class="overview-grid">
            <div class="overview-item"><strong>{{ overview.users }}</strong> 总用户</div>
            <div class="overview-item"><strong>{{ overview.words }}</strong> 总词汇</div>
            <div class="overview-item"><strong>{{ overview.active }}</strong> 今日活跃</div>
          </div>
        </div>

        <div v-if="activeTab === 'users'" class="card">
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
                <td>{{ u.created_at }}</td>
                <td><span class="badge" :class="u.active ? 'badge-green' : 'badge-gray'">{{ u.active ? '激活' : '禁用' }}</span></td>
                <td><button class="btn btn-sm">{{ u.active ? '禁用' : '启用' }}</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="activeTab === 'words'" class="card">
          <div class="admin-table-header">
            <h3>词库管理</h3>
            <div>
              <button class="btn btn-sm btn-primary" @click="showImport = !showImport">批量导入</button>
              <button class="btn btn-sm btn-success">新增单词</button>
            </div>
          </div>
          <div v-if="showImport" style="margin:12px 0;padding:12px;background:var(--color-bg);border-radius:var(--radius-sm)">
            <textarea class="input" rows="4" placeholder="每行一个单词: word,pos,meaning_cn" style="font-family:monospace"></textarea>
            <button class="btn btn-sm btn-primary" style="margin-top:8px">导入</button>
          </div>
          <table class="table">
            <thead><tr><th>单词</th><th>词性</th><th>释义</th><th>来源</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="w in wordList" :key="w.id">
                <td><strong>{{ w.word }}</strong></td>
                <td>{{ w.pos }}</td>
                <td>{{ w.meaning }}</td>
                <td><span class="badge badge-gray">{{ w.source }}</span></td>
                <td><button class="btn btn-sm">编辑</button><button class="btn btn-sm btn-danger" style="margin-left:4px">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="activeTab === 'feedback'" class="card">
          <h3>用户反馈</h3>
          <table class="table">
            <thead><tr><th>用户</th><th>实体</th><th>评分</th><th>反馈</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="f in feedbacks" :key="f.id">
                <td>{{ f.user }}</td>
                <td>{{ f.entity }}</td>
                <td>{{ '⭐'.repeat(f.rating) }}</td>
                <td>{{ f.feedback }}</td>
                <td>{{ f.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>

      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const activeTab = ref('overview')
const userSearch = ref('')
const showImport = ref(false)

const tabs = [
  { key: 'overview', label: '📊 总览' },
  { key: 'users', label: '👥 用户' },
  { key: 'words', label: '📖 词库' },
  { key: 'feedback', label: '💬 反馈' },
]

const overview = ref({ users: 1234, words: 5678, active: 89 })

const users = ref([
  { id: 'u1', username: 'admin', role: 'admin', created_at: '2026-01-01', active: true },
  { id: 'u2', username: 'demo', role: 'user', created_at: '2026-05-20', active: true },
])

const filteredUsers = computed(() =>
  users.value.filter(u => u.username.includes(userSearch.value))
)

const wordList = ref([
  { id: 'w1', word: 'abandon', pos: 'vt.', meaning: '放弃', source: 'CET-4' },
  { id: 'w2', word: 'remarkable', pos: 'adj.', meaning: '显著的', source: 'CET-6' },
])

const feedbacks = ref([
  { id: 'f1', user: 'demo', entity: '例句 #e1', rating: 5, feedback: '非常实用的例句！', time: '2026-05-21' },
])
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
