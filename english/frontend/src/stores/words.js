import { defineStore } from 'pinia'
import { ref } from 'vue'
import { searchApi, wordApi } from '@/api'

export const useWordStore = defineStore('words', () => {
  const searchResults = ref([])
  const searchPagination = ref(null)
  const searchHistory = ref([])
  const currentWord = ref(null)
  const loadingSearch = ref(false)
  const loadingDetail = ref(false)

  async function search(params) {
    loadingSearch.value = true
    try {
      const res = await searchApi.search(params)
      searchResults.value = res.data || res.list || res
      searchPagination.value = res.pagination || res.meta || null
    } finally {
      loadingSearch.value = false
    }
  }

  async function getSuggestions(query, limit) {
    return await searchApi.suggest(query, limit)
  }

  async function fetchWordDetail(id) {
    loadingDetail.value = true
    try {
      currentWord.value = await wordApi.getDetail(id)
    } finally {
      loadingDetail.value = false
    }
  }

  async function updateFrequency(id, freq) {
    await wordApi.updateFrequency(id, freq)
  }

  async function updateNote(id, content, isPrivate) {
    await wordApi.updateNote(id, content, isPrivate)
  }

  async function addTag(id, tagId) {
    await wordApi.addTag(id, tagId)
  }

  async function removeTag(id, tagId) {
    await wordApi.removeTag(id, tagId)
  }

  async function rateWord(id, rating) {
    await wordApi.rate(id, rating)
  }

  async function fetchSearchHistory(limit) {
    searchHistory.value = await searchApi.getHistory(limit)
  }

  async function saveSearchHistory(query, resultCount) {
    await searchApi.saveHistory(query, resultCount)
  }

  async function clearSearchHistory() {
    await searchApi.clearHistory()
    searchHistory.value = []
  }

  return {
    searchResults, searchPagination, searchHistory, currentWord,
    loadingSearch, loadingDetail,
    search, getSuggestions, fetchWordDetail,
    updateFrequency, updateNote, addTag, removeTag, rateWord,
    fetchSearchHistory, saveSearchHistory, clearSearchHistory,
  }
})
