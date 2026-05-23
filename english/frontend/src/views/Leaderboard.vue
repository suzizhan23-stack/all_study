<template>
  <div class="leaderboard-page">
    <h2 style="margin-bottom:16px">🏆 排行榜</h2>
    <div class="lb-tabs" style="margin-bottom:16px">
      <button
        v-for="tab in tabs" :key="tab.key"
        class="btn btn-sm"
        :class="{ 'btn-primary': activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >{{ tab.label }}</button>
      <span v-if="myRank" style="margin-left:auto;font-size:14px;color:var(--color-text-secondary)">你的排名: #{{ myRank.rank || myRank }}</span>
    </div>
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>
    <table class="table">
      <thead>
        <tr>
          <th style="width:50px">排名</th>
          <th>用户</th>
          <th>等级</th>
          <th>XP</th>
          <th>打卡</th>
          <th>正确率</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(u, i) in rankings" :key="u.id" :class="{ 'current-user': u.id === currentUserId }">
          <td><span class="rank-badge" :class="'rank-' + (i + 1)">{{ medal(i + 1) }}{{ i + 1 }}</span></td>
          <td>
            <div class="lb-user">
              <span class="lb-avatar">{{ u.avatar }}</span>
              <span>{{ u.username }}</span>
              <span v-if="u.id === currentUserId" class="badge badge-green">你</span>
            </div>
          </td>
          <td>Lv.{{ u.level }}</td>
          <td>{{ u.xp }}</td>
          <td>🔥 {{ u.streak }}天</td>
          <td>👍 {{ u.accuracy }}%</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { leaderboardApi } from '../api'

const store = useUserStore()

const currentUserId = ref('')
const rankings = ref([])
const myRank = ref(null)
const activeTab = ref('global')
const loading = ref(false)

const tabs = [
  { key: 'global', label: '🏅 总榜' },
  { key: 'weekly', label: '🔥 周榜' },
  { key: 'friends', label: '👥 好友' },
]

async function fetchLeaderboard(type) {
  loading.value = true
  try {
    const res = await leaderboardApi.get(type, 100)
    rankings.value = res.list || res.data || res || []
    myRank.value = res.myRank || null
    currentUserId.value = store.user?.id || ''
  } catch {
    rankings.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  fetchLeaderboard(tab)
}

function medal(rank) {
  return rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : ''
}

onMounted(async () => {
  await fetchLeaderboard('global')
})
</script>

<style scoped>
.lb-tabs { display: flex; gap: 4px; }
.rank-badge { display: inline-flex; align-items: center; gap: 4px; font-weight: 600; }
.rank-1 { color: #eab308; }
.rank-2 { color: #9ca3af; }
.rank-3 { color: #d97706; }
.lb-user { display: flex; align-items: center; gap: 8px; }
.lb-avatar { font-size: 20px; }
.current-user { background: var(--color-primary-light); }
</style>
