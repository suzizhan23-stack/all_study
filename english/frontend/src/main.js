import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/style.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

const token = localStorage.getItem('auth_token')
if (token) {
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  userStore.fetchProfile()
}

app.mount('#app')
