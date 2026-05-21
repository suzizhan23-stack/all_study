<template>
  <div class="leaderboard-page">
    <h2 style="margin-bottom:16px">🏆 排行榜</h2>
    <div class="lb-tabs" style="margin-bottom:16px">
      <button class="btn btn-primary btn-sm">🏅 总榜</button>
      <button class="btn btn-sm">🔥 周榜</button>
      <button class="btn btn-sm">👥 好友</button>
    </div>
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
import { ref } from 'vue'

const currentUserId = 'u2'

const rankings = ref([
  { id: 'u1', username: 'admin', avatar: '👑', level: 12, xp: 2450, streak: 15, accuracy: 89 },
  { id: 'u2', username: 'demo', avatar: '👤', level: 5, xp: 320, streak: 3, accuracy: 75 },
  { id: 'u3', username: 'user1', avatar: '😊', level: 4, xp: 280, streak: 5, accuracy: 82 },
  { id: 'u4', username: 'user2', avatar: '😎', level: 3, xp: 150, streak: 2, accuracy: 70 },
])

function medal(rank) {
  return rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : ''
}
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
