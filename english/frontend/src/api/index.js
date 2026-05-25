import { api } from './http'

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
}

export const userApi = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  getSettings: () => api.get('/user/settings'),
  updateSettings: (data) => api.put('/user/settings', { settings: data }),
  getActivity: (days) => api.get('/user/activity', { days }),
  getBadges: () => api.get('/user/badges'),
  getDefaultStrategy: () => api.get('/user/default-strategy'),
  setDefaultStrategy: (strategyId) => api.put('/user/default-strategy', { strategyId }),
  updateStreak: () => api.put('/user/stats/streak'),
}

export const wordApi = {
  getDetail: (id) => api.get(`/words/${id}`),
  updateFrequency: (id, frequency) => api.put(`/words/${id}/frequency`, { frequency }),
  updateNote: (id, content, isPrivate) => api.put(`/words/${id}/note`, { content, isPrivate }),
  addTag: (id, tagId) => api.post(`/words/${id}/tags`, { tagId }),
  removeTag: (id, tagId) => api.delete(`/words/${id}/tags/${tagId}`),
  rate: (id, rating) => api.put(`/words/${id}/rating`, { rating }),
}

export const searchApi = {
  suggest: (query, limit) => api.get('/search/suggest', { query, limit }),
  search: (params) => api.get('/search', params),
  getHistory: (limit) => api.get('/search/history', { limit }),
  saveHistory: (query, resultCount) => api.post('/search/history', { query, resultCount }),
  clearHistory: () => api.delete('/search/history'),
}

export const reviewApi = {
  getQueue: (params) => api.get('/review/queue', params),
  submitResult: (data) => api.post('/review/result', data),
  getDistractors: (params) => api.get('/review/distractors', params),
  getStats: () => api.get('/review/stats'),
}

export const articleApi = {
  getList: (params) => api.get('/articles', params),
  getDetail: (id) => api.get(`/articles/${id}`),
  updateProgress: (id, data) => api.put(`/articles/${id}/progress`, data),
  complete: (id) => api.put(`/articles/${id}/complete`),
  lookup: (id, word) => api.get(`/articles/${id}/lookup`, { word }),
}

export const folderApi = {
  getList: (category) => api.get('/folders', { category }),
  create: (data) => api.post('/folders', data),
  update: (id, data) => api.put(`/folders/${id}`, data),
  delete: (id) => api.delete(`/folders/${id}`),
  reorder: (order) => api.put('/folders/reorder', { order }),
  getItems: (id, params) => api.get(`/folders/${id}/items`, params),
}

export const favoriteApi = {
  add: (data) => api.post('/favorites', data),
  remove: (id) => api.delete(`/favorites/${id}`),
  batchDelete: (ids) => api.post('/favorites/batch-delete', { ids }),
  batchTag: (wordIds, tagId) => api.post('/favorites/batch-tag', { wordIds, tagId }),
}

export const wordBookApi = {
  getList: (difficultyLevel) => api.get('/word-books', { difficultyLevel }),
  getWords: (id, params) => api.get(`/word-books/${id}/words`, params),
  getPosCategories: () => api.get('/word-books/pos-categories'),
}

export const planApi = {
  getActive: () => api.get('/plans/active'),
  getTemplates: () => api.get('/plans/templates'),
  join: (planId) => api.post('/plans/join', { planId }),
  create: (data) => api.post('/plans/create', data),
  getDailyWords: (date) => api.get('/plans/daily/words', { date }),
  getDailyDates: (limit) => api.get('/plans/daily/dates', { limit }),
  addEntry: (data) => api.post('/plans/daily/entries', data),
  batchAddEntries: (data) => api.post('/plans/daily/entries/batch', data),
  deleteEntry: (id) => api.delete(`/plans/daily/entries/${id}`),
  completeEntry: (id) => api.put(`/plans/daily/entries/${id}/complete`),
  toggleKeyPoint: (id) => api.put(`/plans/daily/entries/${id}/key-point`),
  toggleKeyPointByWord: (wordId) => api.put(`/plans/daily/entries/by-word/${wordId}/key-point`),
  generate: (data) => api.post('/plans/daily/generate', data),
  advanceDay: () => api.post('/plans/advance'),
  setCurrentWordBook: (data) => api.put('/plans/current-wordbook', data),
}

export const dashboardApi = {
  get: () => api.get('/dashboard'),
  consumeRecommendation: (id) => api.put(`/dashboard/recommendations/${id}/consume`),
}

export const leaderboardApi = {
  get: (type, limit) => api.get('/leaderboard', { type, limit }),
}

export const wrongWordApi = {
  getList: (params) => api.get('/wrong-words', params),
  getReviewWords: (data) => api.post('/wrong-words/review', data),
}

export const tagApi = {
  getList: () => api.get('/tags'),
  create: (data) => api.post('/tags', data),
}

export const strategyApi = {
  getList: () => api.get('/strategies'),
}

export const badgeApi = {
  getList: () => api.get('/badges'),
}

export const adminApi = {
  getOverview: () => api.get('/admin/overview'),
  getUsers: (params) => api.get('/admin/users', params),
  toggleUserStatus: (id, isActive) => api.put(`/admin/users/${id}/status`, { isActive }),
  getWords: (params) => api.get('/admin/words', params),
  createWord: (data) => api.post('/admin/words', data),
  updateWord: (id, data) => api.put(`/admin/words/${id}`, data),
  deleteWord: (id) => api.delete(`/admin/words/${id}`),
  batchImport: (data) => api.post('/admin/words/batch-import', data),
  getFeedback: (params) => api.get('/admin/feedback', params),
  updateCollocation: (id, data) => api.put(`/admin/collocations/${id}`, data),
  deleteCollocation: (id) => api.delete(`/admin/collocations/${id}`),
  updatePrepPattern: (id, data) => api.put(`/admin/prep-patterns/${id}`, data),
  deletePrepPattern: (id) => api.delete(`/admin/prep-patterns/${id}`),
}
