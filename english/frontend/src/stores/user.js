import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, userApi, badgeApi } from '@/api'
import { setToken, removeToken } from '@/api/http'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const settings = ref({})
  const activity = ref([])
  const badges = ref({ list: [], earnedCount: 0, totalCount: 0 })
  const isLoggedIn = ref(false)
  const loading = ref(false)

  async function login(username, password) {
    loading.value = true
    try {
      const res = await authApi.login({ username, password })
      setToken(res.token)
      isLoggedIn.value = true
      await fetchProfile()
    } finally {
      loading.value = false
    }
  }

  async function register(data) {
    loading.value = true
    try {
      const res = await authApi.register(data)
      setToken(res.token)
      isLoggedIn.value = true
      await fetchProfile()
    } finally {
      loading.value = false
    }
  }

  function logout() {
    removeToken()
    user.value = null
    settings.value = {}
    activity.value = []
    badges.value = { list: [], earnedCount: 0, totalCount: 0 }
    isLoggedIn.value = false
  }

  async function fetchProfile() {
    try {
      const data = await userApi.getProfile()
      user.value = data
      isLoggedIn.value = true
    } catch {
      removeToken()
      user.value = null
      isLoggedIn.value = false
    }
  }

  async function fetchSettings() {
    settings.value = await userApi.getSettings()
  }

  async function updateSettings(data) {
    await userApi.updateSettings(data)
    settings.value = { ...settings.value, ...data }
  }

  async function fetchActivity(days = 7) {
    activity.value = await userApi.getActivity(days)
  }

  async function fetchBadges() {
    const data = await badgeApi.getList()
    if (Array.isArray(data)) {
      badges.value = { list: data, earnedCount: 0, totalCount: data.length }
    } else {
      badges.value = data
    }
  }

  async function fetchDefaultStrategy() {
    return await userApi.getDefaultStrategy()
  }

  async function setDefaultStrategy(id) {
    await userApi.setDefaultStrategy(id)
  }

  async function updateStreak() {
    await userApi.updateStreak()
  }

  return {
    user, settings, activity, badges, isLoggedIn, loading,
    login, register, logout, fetchProfile, fetchSettings, updateSettings,
    fetchActivity, fetchBadges, fetchDefaultStrategy, setDefaultStrategy, updateStreak,
  }
})
