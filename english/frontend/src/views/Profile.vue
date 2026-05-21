<template>
  <div class="profile-page">
    <div class="profile-grid">
      <div class="card profile-info">
        <div class="avatar">🖼️</div>
        <h2>{{ user.nickname }}</h2>
        <div class="profile-level">Lv.{{ user.level }} · {{ user.xp }} XP</div>
        <p class="profile-bio">{{ user.bio }}</p>
        <button class="btn btn-sm">编辑资料</button>
        <div class="profile-email">📧 {{ user.email }}</div>
      </div>

      <div class="card profile-stats">
        <h3>学习统计</h3>
        <div class="stats-grid">
          <div class="pstat"><strong>{{ user.totalWords }}</strong><span>累计学习</span></div>
          <div class="pstat"><strong>{{ user.totalReviews }}</strong><span>累计复习</span></div>
          <div class="pstat"><strong>{{ user.totalHours }}h</strong><span>累计时长</span></div>
          <div class="pstat"><strong>🔥 {{ user.streak }}</strong><span>当前打卡</span></div>
        </div>
        <div class="chart-placeholder">
          <div class="chart-label">近 7 天学习曲线</div>
          <div class="mini-chart">
            <div v-for="(d, i) in weekData" :key="i" class="bar" :style="{ height: d * 2 + 'px' }" :title="d + ' 词'"></div>
          </div>
          <div class="chart-days">
            <span v-for="(day, i) in ['一','二','三','四','五','六','日']" :key="i">{{ day }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>偏好设置</h3>
      <div class="settings-list">
        <div v-for="s in settings" :key="s.key" class="setting-item">
          <label>{{ s.label }}</label>
          <input v-if="s.type === 'number'" v-model.number="s.val" class="input" type="number" style="max-width:100px" />
          <select v-else-if="s.type === 'select'" v-model="s.val" class="input" style="max-width:180px">
            <option v-for="o in s.options" :key="o" :value="o">{{ o }}</option>
          </select>
          <input v-else-if="s.type === 'time'" v-model="s.val" class="input" type="time" style="max-width:120px" />
        </div>
      </div>
      <button class="btn btn-primary" style="margin-top:12px" @click="saveSettings">保存设置</button>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>徽章墙</h3>
      <div class="badges-grid">
        <div v-for="b in badges" :key="b.id" class="badge-item" :class="{ 'badge-earned': b.earned, 'badge-locked': !b.earned }">
          <div class="badge-icon">{{ b.icon }}</div>
          <div class="badge-name">{{ b.name }}</div>
          <div class="badge-status">{{ b.earned ? '已获得' : '未获得' }}</div>
        </div>
      </div>
      <router-link to="/leaderboard" class="btn btn-sm" style="margin-top:12px">🏆 排行榜</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../stores/user'

const store = useUserStore()

const user = ref({
  nickname: store.nickname,
  level: store.level,
  xp: store.xp,
  bio: store.bio,
  email: 'demo@example.com',
  totalWords: store.totalWordsLearned,
  totalReviews: store.totalReviews,
  totalHours: Math.round(store.totalTimeSpentSec / 3600 * 10) / 10,
  streak: store.streakDays,
})

const weekData = ref([12, 18, 6, 24, 15, 8, 12])

const settings = ref([
  { key: 'daily_word_goal', label: '每日目标', type: 'number', val: parseInt(store.settings.daily_word_goal) },
  { key: 'learning_mode', label: '学习模式', type: 'select', val: store.settings.learning_mode, options: ['card', 'choice', 'spelling', 'listening'] },
  { key: 'pronunciation', label: '发音偏好', type: 'select', val: store.settings.pronunciation, options: ['uk', 'us'] },
  { key: 'theme', label: '主题', type: 'select', val: store.settings.theme, options: ['light', 'dark'] },
  { key: 'reminder_time', label: '提醒时间', type: 'time', val: store.settings.reminder_time },
  { key: 'ui_language', label: '界面语言', type: 'select', val: store.settings.ui_language, options: ['zh', 'en'] },
])

const badges = ref([
  { id: 'b1', name: '初次学习', icon: '🎯', earned: true },
  { id: 'b2', name: '打卡7天', icon: '🔥', earned: false },
  { id: 'b3', name: '百词斩', icon: '💪', earned: false },
  { id: 'b4', name: '学霸', icon: '🧠', earned: false },
  { id: 'b5', name: '书虫', icon: '📚', earned: false },
])

function saveSettings() {
  settings.value.forEach(s => store.updateSetting(s.key, s.val))
  alert('设置已保存')
}
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 16px; }
.profile-info { text-align: center; padding: 32px; }
.avatar { font-size: 64px; margin-bottom: 12px; }
.profile-level { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 8px; }
.profile-bio { font-size: 14px; margin-bottom: 12px; color: var(--color-text-secondary); }
.profile-email { font-size: 13px; color: var(--color-text-secondary); margin-top: 12px; }
.profile-stats h3 { margin-bottom: 16px; }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.pstat { text-align: center; padding: 12px; background: var(--color-bg); border-radius: var(--radius-sm); }
.pstat strong { display: block; font-size: 24px; }
.pstat span { font-size: 13px; color: var(--color-text-secondary); }
.chart-placeholder { margin-top: 20px; }
.chart-label { font-size: 14px; font-weight: 500; margin-bottom: 8px; }
.mini-chart { display: flex; align-items: flex-end; gap: 8px; height: 60px; padding: 8px 0; }
.bar { flex: 1; background: var(--color-primary); border-radius: 4px 4px 0 0; min-height: 4px; opacity: .8; }
.chart-days { display: flex; gap: 8px; font-size: 12px; color: var(--color-text-secondary); }
.chart-days span { flex: 1; text-align: center; }
.settings-list { display: flex; flex-direction: column; gap: 12px; }
.setting-item { display: flex; align-items: center; gap: 16px; }
.setting-item label { min-width: 100px; font-size: 14px; font-weight: 500; }
.badges-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.badge-item { text-align: center; padding: 16px; border-radius: var(--radius); border: 1px solid var(--color-border); width: 100px; }
.badge-earned { background: var(--color-primary-light); }
.badge-locked { opacity: .5; }
.badge-icon { font-size: 28px; margin-bottom: 4px; }
.badge-name { font-size: 13px; font-weight: 500; }
.badge-status { font-size: 12px; color: var(--color-text-secondary); }
</style>
