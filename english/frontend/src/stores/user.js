import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const id = ref('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12')
  const username = ref('demo')
  const nickname = ref('演示用户')
  const avatarUrl = ref('')
  const bio = ref('英语学习者')
  const level = ref(5)
  const xp = ref(320)
  const streakDays = ref(3)
  const longestStreak = ref(15)
  const totalWordsLearned = ref(320)
  const totalReviews = ref(1245)
  const totalTimeSpentSec = ref(102600)

  const settings = ref({
    daily_word_goal: 20,
    learning_mode: 'card',
    pronunciation: 'uk',
    theme: 'light',
    reminder_time: '08:00',
    auto_play_audio: 'true',
    ui_language: 'zh',
  })

  function updateSetting(key, val) { settings.value[key] = val }

  return {
    id, username, nickname, avatarUrl, bio,
    level, xp, streakDays, longestStreak,
    totalWordsLearned, totalReviews, totalTimeSpentSec,
    settings, updateSetting,
  }
})
