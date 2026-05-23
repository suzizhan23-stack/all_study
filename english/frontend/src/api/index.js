import { api } from './http'

export const authApi = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
  logout: () => api.post('/api/auth/logout'),
}

export const userApi = {
  getProfile: () => api.get('/api/user/profile'),
  updateProfile: (data) => api.put('/api/user/profile', data),
  getSettings: () => api.get('/api/user/settings'),
  updateSettings: (data) => api.put('/api/user/settings', { settings: data }),
  getActivity: (days) => api.get('/api/user/activity', { days }),
  getBadges: () => api.get('/api/user/badges'),
  getDefaultStrategy: () => api.get('/api/user/default-strategy'),
  setDefaultStrategy: (strategyId) => api.put('/api/user/default-strategy', { strategyId }),
  updateStreak: () => api.put('/api/user/stats/streak'),
}

export const wordApi = {
  getDetail: (id) => api.get(`/api/words/${id}`),
  updateFrequency: (id, frequency) => api.put(`/api/words/${id}/frequency`, { frequency }),
  updateNote: (id, content, isPrivate) => api.put(`/api/words/${id}/note`, { content, isPrivate }),
  addTag: (id, tagId) => api.post(`/api/words/${id}/tags`, { tagId }),
  removeTag: (id, tagId) => api.delete(`/api/words/${id}/tags/${tagId}`),
  rate: (id, rating) => api.put(`/api/words/${id}/rating`, { rating }),
}

export const searchApi = {
  suggest: (query, limit) => api.get('/api/search/suggest', { query, limit }),
  search: (params) => api.get('/api/search', params),
  getHistory: (limit) => api.get('/api/search/history', { limit }),
  saveHistory: (query, resultCount) => api.post('/api/search/history', { query, resultCount }),
  clearHistory: () => api.delete('/api/search/history'),
}

export const reviewApi = {
  getQueue: (params) => api.get('/api/review/queue', params),
  submitResult: (data) => api.post('/api/review/result', data),
  getDistractors: (params) => api.get('/api/review/distractors', params),
  getStats: () => api.get('/api/review/stats'),
}

export const articleApi = {
  getList: (params) => api.get('/api/articles', params),
  getDetail: (id) => api.get(`/api/articles/${id}`),
  updateProgress: (id, data) => api.put(`/api/articles/${id}/progress`, data),
  complete: (id) => api.put(`/api/articles/${id}/complete`),
  lookup: (id, word) => api.get(`/api/articles/${id}/lookup`, { word }),
}

export const folderApi = {
  getList: (category) => api.get('/api/folders', { category }),
  create: (data) => api.post('/api/folders', data),
  update: (id, data) => api.put(`/api/folders/${id}`, data),
  delete: (id) => api.delete(`/api/folders/${id}`),
  reorder: (order) => api.put('/api/folders/reorder', { order }),
  getItems: (id, params) => api.get(`/api/folders/${id}/items`, params),
}

export const favoriteApi = {
  add: (data) => api.post('/api/favorites', data),
  remove: (id) => api.delete(`/api/favorites/${id}`),
  batchDelete: (ids) => api.post('/api/favorites/batch-delete', { ids }),
  batchTag: (wordIds, tagId) => api.post('/api/favorites/batch-tag', { wordIds, tagId }),
}

export const wordBookApi = {
  getList: (difficultyLevel) => api.get('/api/word-books', { difficultyLevel }),
  getWords: (id, params) => api.get(`/api/word-books/${id}/words`, params),
  getPosCategories: () => api.get('/api/word-books/pos-categories'),
}

export const planApi = {
  getActive: () => api.get('/api/plans/active'),
  getTemplates: () => api.get('/api/plans/templates'),
  join: (planId) => api.post('/api/plans/join', { planId }),
  getDailyWords: (date) => api.get('/api/plans/daily/words', { date }),
  getDailyDates: (limit) => api.get('/api/plans/daily/dates', { limit }),
  addEntry: (data) => api.post('/api/plans/daily/entries', data),
  deleteEntry: (id) => api.delete(`/api/plans/daily/entries/${id}`),
  completeEntry: (id) => api.put(`/api/plans/daily/entries/${id}/complete`),
  generate: (data) => api.post('/api/plans/daily/generate', data),
}

export const dashboardApi = {
  get: () => api.get('/api/dashboard'),
  consumeRecommendation: (id) => api.put(`/api/dashboard/recommendations/${id}/consume`),
}

export const leaderboardApi = {
  get: (type, limit) => api.get('/api/leaderboard', { type, limit }),
}

export const wrongWordApi = {
  getList: (params) => api.get('/api/wrong-words', params),
  getReviewWords: (data) => api.post('/api/wrong-words/review', data),
}

export const tagApi = {
  getList: () => api.get('/api/tags'),
  create: (data) => api.post('/api/tags', data),
}

export const strategyApi = {
  getList: () => api.get('/api/strategies'),
}

export const badgeApi = {
  getList: () => api.get('/api/badges'),
}

export const adminApi = {
  getOverview: () => api.get('/api/admin/overview'),
  getUsers: (params) => api.get('/api/admin/users', params),
  toggleUserStatus: (id, isActive) => api.put(`/api/admin/users/${id}/status`, { isActive }),
  getWords: (params) => api.get('/api/admin/words', params),
  createWord: (data) => api.post('/api/admin/words', data),
  updateWord: (id, data) => api.put(`/api/admin/words/${id}`, data),
  deleteWord: (id) => api.delete(`/api/admin/words/${id}`),
  batchImport: (data) => api.post('/api/admin/words/batch-import', data),
  getFeedback: (params) => api.get('/api/admin/feedback', params),
}
