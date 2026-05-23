import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wordBookApi, strategyApi } from '@/api'

export const useWordBookStore = defineStore('wordBooks', () => {
  const books = ref([])
  const strategies = ref([])
  const posCategories = ref([])
  const currentBookWords = ref([])
  const currentBookPagination = ref(null)
  const loading = ref(false)

  async function fetchBooks(difficultyLevel) {
    loading.value = true
    try {
      books.value = await wordBookApi.getList(difficultyLevel)
    } finally {
      loading.value = false
    }
  }

  async function fetchStrategies() {
    strategies.value = await strategyApi.getList()
  }

  async function fetchPosCategories() {
    const data = await wordBookApi.getPosCategories()
    posCategories.value = data
  }

  async function fetchBookWords(id, params) {
    loading.value = true
    try {
      const res = await wordBookApi.getWords(id, params)
      currentBookWords.value = res.data || res.list || res
      currentBookPagination.value = res.pagination || res.meta || null
    } finally {
      loading.value = false
    }
  }

  return {
    books, strategies, posCategories, currentBookWords, currentBookPagination, loading,
    fetchBooks, fetchStrategies, fetchPosCategories, fetchBookWords,
  }
})
