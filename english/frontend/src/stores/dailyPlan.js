import { defineStore } from 'pinia'
import { ref } from 'vue'
import { planApi } from '@/api'

export const useDailyPlanStore = defineStore('dailyPlan', () => {
  const plans = ref([])
  const templates = ref([])
  const activePlan = ref(null)
  const dailyWords = ref([])
  const dailyDates = ref([])
  const loading = ref(false)

  async function fetchActivePlan() {
    loading.value = true
    try {
      activePlan.value = await planApi.getActive()
    } finally {
      loading.value = false
    }
  }

  async function fetchTemplates() {
    templates.value = await planApi.getTemplates()
  }

  async function joinPlan(planId) {
    await planApi.join(planId)
  }

  async function fetchDailyWords(date) {
    const res = await planApi.getDailyWords(date)
    dailyWords.value = res.words || []
  }

  async function fetchDailyDates(limit) {
    const res = await planApi.getDailyDates(limit)
    dailyDates.value = (res.dates || []).map(d => d.date)
  }

  async function addEntry(data) {
    await planApi.addEntry(data)
  }

  async function deleteEntry(id) {
    await planApi.deleteEntry(id)
  }

  async function completeEntry(id) {
    await planApi.completeEntry(id)
  }

  async function generate(data) {
    loading.value = true
    try {
      return await planApi.generate(data)
    } finally {
      loading.value = false
    }
  }

  return {
    plans, templates, activePlan, dailyWords, dailyDates, loading,
    fetchActivePlan, fetchTemplates, joinPlan, fetchDailyWords, fetchDailyDates,
    addEntry, deleteEntry, completeEntry, generate,
  }
})
