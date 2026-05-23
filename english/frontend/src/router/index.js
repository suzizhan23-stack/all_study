import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
  { path: '/search', name: 'Search', component: () => import('@/views/Search.vue') },
  { path: '/word/:id', name: 'WordDetail', component: () => import('@/views/WordDetail.vue') },
  { path: '/learn', name: 'Learning', component: () => import('@/views/Learning.vue') },
  { path: '/review', name: 'Review', component: () => import('@/views/Review.vue') },
  { path: '/reading', name: 'Reading', component: () => import('@/views/Reading.vue') },
  { path: '/reading/:id', name: 'ReadingArticle', component: () => import('@/views/ReadingArticle.vue') },
  { path: '/favorites', name: 'Favorites', component: () => import('@/views/Favorites.vue') },
  { path: '/favorites/:id', name: 'FavoriteFolder', component: () => import('@/views/FavoriteFolder.vue') },
  { path: '/wrong-answers', name: 'WrongAnswers', component: () => import('@/views/WrongAnswers.vue') },
  { path: '/word-books', name: 'WordBooks', component: () => import('@/views/WordBooks.vue') },
  { path: '/plans', name: 'StudyPlans', component: () => import('@/views/StudyPlans.vue') },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue') },
  { path: '/leaderboard', name: 'Leaderboard', component: () => import('@/views/Leaderboard.vue') },
  { path: '/admin', name: 'Admin', component: () => import('@/views/Admin.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.name !== 'Login' && !getToken()) {
    return { name: 'Login' }
  }
})

export default router
