<template>
  <div class="dashboard">
    <div class="stats-row">
      <div class="card stat-card">
        <div class="stat-label">今日学习</div>
        <div class="stat-value">{{ todayWords }}<span class="stat-unit">/{{ goal }} 词</span></div>
        <div class="progress-bar" style="margin-top:8px">
          <div class="progress-bar-fill green" :style="{ width: goalPct + '%' }"></div>
        </div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">连续打卡</div>
        <div class="stat-value fire">🔥 {{ userStore.streakDays }}<span class="stat-unit">天</span></div>
        <div style="font-size:13px;color:var(--color-text-secondary);margin-top:4px">最长 {{ userStore.longestStreak }} 天</div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">等级</div>
        <div class="stat-value">Lv.{{ userStore.level }} <span class="stat-unit">{{ userStore.xp }} XP</span></div>
        <div class="progress-bar" style="margin-top:8px">
          <div class="progress-bar-fill orange" style="width:60%"></div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:20px">
      <div class="section-header">
        <h3>今日推荐</h3>
        <router-link to="/learn" class="btn btn-sm">更多 →</router-link>
      </div>
      <div class="recommend-list">
        <div v-for="item in recommendations" :key="item.id" class="recommend-item">
          <span class="recommend-icon">{{ item.icon }}</span>
          <div class="recommend-info">
            <strong>{{ item.word }}</strong>
            <span class="recommend-reason">{{ item.reason }}</span>
          </div>
          <button class="btn btn-sm btn-primary" @click="startReview(item)">{{ item.action }}</button>
          <span class="recommend-star">👍👍👍</span>
        </div>
      </div>
    </div>

    <div class="quick-grid" style="margin-top:20px">
      <router-link to="/learn" class="card quick-card">
        <div class="quick-icon">🎴</div>
        <div class="quick-label">继续学习</div>
        <div class="quick-sub">{{ dueCount }} 个词待复习</div>
      </router-link>
      <router-link to="/reading" class="card quick-card">
        <div class="quick-icon">📰</div>
        <div class="quick-label">文章推荐</div>
        <div class="quick-sub">{{ unreadCount }} 篇未读完</div>
      </router-link>
      <router-link to="/wrong-answers" class="card quick-card">
        <div class="quick-icon">❌</div>
        <div class="quick-label">错题回顾</div>
        <div class="quick-sub">{{ wrongCount }} 个易错词</div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const goal = computed(() => parseInt(userStore.settings.daily_word_goal) || 20)
const todayWords = ref(12)
const goalPct = computed(() => Math.min(100, (todayWords.value / goal.value) * 100))
const dueCount = ref(5)
const unreadCount = ref(3)
const wrongCount = ref(2)

const recommendations = ref([
  { id: 1, word: 'abandon', reason: '间隔复习到期', action: '复习', icon: '📌' },
  { id: 2, word: 'remarkable', reason: '易错词巩固', action: '复习', icon: '📌' },
  { id: 3, word: 'contribute', reason: '推荐新词', action: '学习', icon: '📌' },
])

function startReview(item) {
  router.push('/learn')
}
</script>

<style scoped>
.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.stat-card { text-align: center; padding: 24px; }
.stat-label { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-value.fire { color: #f97316; }
.stat-unit { font-size: 14px; font-weight: 400; color: var(--color-text-secondary); margin-left: 4px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { font-size: 16px; font-weight: 600; }
.recommend-list { display: flex; flex-direction: column; gap: 0; }
.recommend-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}
.recommend-item:last-child { border-bottom: none; }
.recommend-icon { font-size: 20px; }
.recommend-info { flex: 1; }
.recommend-info strong { display: block; font-size: 15px; }
.recommend-reason { font-size: 13px; color: var(--color-text-secondary); }
.recommend-star { color: #f59e0b; font-size: 13px; }
.quick-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.quick-card { text-align: center; padding: 24px; transition: .15s; }
.quick-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); text-decoration: none; }
.quick-icon { font-size: 32px; margin-bottom: 8px; }
.quick-label { font-size: 15px; font-weight: 600; }
.quick-sub { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }
</style>
