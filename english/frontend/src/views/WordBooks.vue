<template>
  <div class="wordbooks-page">
    <h2 style="margin-bottom:16px">📚 单词本</h2>

    <!-- 单词本列表 -->
    <div class="grid-3" style="margin-bottom:24px">
      <div
        v-for="b in store.books"
        :key="b.id"
        class="card book-card"
        :class="{ 'book-active': selectedBook === b.id, 'book-bound': boundBookId === b.id }"
        @click="selectedBook = b.id"
      >
        <div class="book-name">{{ b.name }}</div>
        <div class="book-level badge" :class="levelBadge(b.difficulty_level)">{{ b.difficulty_level }}</div>
        <div class="book-count">{{ b.word_count }} 词</div>
        <div class="book-desc">{{ b.description }}</div>
        <div v-if="boundBookId === b.id" class="bound-tag">当前学习</div>
      </div>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>

  <!-- 当前选中的单词本 -->
    <template v-if="currentBook">
      <!-- 筛选 -->
      <div class="card" style="margin-bottom:16px">
        <div class="book-filter-bar">
          <div class="filter-group">
            <label>词性：</label>
            <button
              v-for="cat in store.posCategories"
              :key="cat.value"
              class="btn btn-sm"
              :class="{ 'btn-primary': selectedPos.includes(cat.value) }"
              @click="togglePos(cat)"
            >{{ cat.label }}</button>
            <button v-if="selectedPos.length" class="btn btn-sm" @click="selectedPos = []; currentPage = 1; fetchWords()">清除</button>
          </div>
          <div class="filter-group" style="margin-top:8px">
            <label>首字母：</label>
            <button
              v-for="l in letters"
              :key="l"
              class="btn btn-sm letter-btn"
              :class="{ 'btn-primary': selectedLetter === l }"
              @click="toggleLetter(l)"
            >{{ l }}</button>
            <button v-if="selectedLetter" class="btn btn-sm" @click="selectedLetter = ''">清除</button>
          </div>
        </div>
      </div>

      <!-- 预览词条 -->
      <div class="card" style="margin-bottom:16px">
        <div class="preview-header">
          <h3>{{ currentBook.name }} · 预览</h3>
          <div style="display:flex;align-items:center;gap:8px">
            <span class="preview-count">{{ (store.currentBookWords || []).length }} 个词</span>
            <button v-if="store.currentBookWords?.length" class="btn btn-sm btn-primary" @click="addAllToPlan">一键全部加入</button>
          </div>
        </div>
        <table class="table">
          <thead>
            <tr><th>#</th><th>单词</th><th>词性</th><th>释义</th><th>首字母</th><th>难度</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="(w, i) in store.currentBookWords" :key="w.id">
              <td>{{ i + 1 }}</td>
              <td>
                <router-link :to="`/word/${w.id}`" class="word-link">{{ w.word }}</router-link>
                <span v-if="planCount(w.id) > 0" class="plan-count-badge" :title="`已加入 ${planCount(w.id)} 次`">{{ planCount(w.id) }}</span>
              </td>
              <td><span class="badge badge-blue">{{ w.pos }}</span></td>
              <td>{{ w.meaning_cn }}</td>
              <td><span class="badge badge-gray">{{ w.first_letter }}</span></td>
              <td>{{ '⭐'.repeat(w.difficulty) }}</td>
              <td class="action-cell">
                <button v-if="!inToday(w.id)" class="btn-add" title="加入学习计划" @click="addToPlan(w)">+</button>
                <span v-else class="check-tag">✓</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="pagination" v-if="totalPages > 1">
          <button class="btn btn-sm" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">‹</button>
          <span class="page-info">
            第 <input v-model.number="jumpPage" class="input page-jump" type="number" :min="1" :max="totalPages" @keyup.enter="goJump" /> / {{ totalPages }} 页
          </span>
          <button class="btn btn-sm" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">›</button>
          <select v-model.number="pageSize" class="input page-size" @change="onPageSizeChange">
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
          </select>
        </div>
        <div v-if="!store.currentBookWords || !store.currentBookWords.length" class="empty-state" style="padding:40px">
          <p>暂无匹配的词条</p>
        </div>
      </div>
    </template>

    <!-- Toast -->
    <div v-if="toast.show" class="toast" :class="'toast-' + toast.type" @click="toast.show = false">
      {{ toast.msg }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useWordBookStore } from '../stores/wordBooks'
import { planApi } from '../api'

const store = useWordBookStore()

const selectedBook = ref('')
const boundBookId = ref('')
const selectedPos = ref([])
const selectedLetter = ref('')
const loading = ref(false)
const todayEntries = ref(new Set())
const currentPage = ref(1)
const pageSize = ref(20)
const jumpPage = ref(1)

const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

const toast = ref({ show: false, msg: '', type: 'success' })
let toastTimer = null

function showToast(msg, type = 'success') {
  toast.value = { show: true, msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 2000)
}

const currentBook = computed(() => store.books.find(b => b.id === selectedBook.value))

async function fetchWords() {
  if (!selectedBook.value) return
  const posValues = selectedPos.value.map(v => {
    const cat = store.posCategories.find(c => c.value === v)
    return cat ? cat.posList : [v]
  }).flat().join(',')
  await store.fetchBookWords(selectedBook.value, {
    pos: posValues || undefined,
    letter: selectedLetter.value || undefined,
    page: currentPage.value,
    size: pageSize.value,
  })
}

function togglePos(cat) {
  const idx = selectedPos.value.indexOf(cat.value)
  if (idx >= 0) {
    selectedPos.value.splice(idx, 1)
  } else {
    selectedPos.value.push(cat.value)
  }
  currentPage.value = 1
  fetchWords()
}
function toggleLetter(l) {
  selectedLetter.value = selectedLetter.value === l ? '' : l
  currentPage.value = 1
  fetchWords()
}

const totalPages = computed(() =>
  store.currentBookPagination ? store.currentBookPagination.totalPages : 0
)

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
  jumpPage.value = p
  fetchWords()
}

function goJump() {
  const p = jumpPage.value
  if (!p || p < 1) { jumpPage.value = 1; return }
  if (p > totalPages.value) { jumpPage.value = totalPages.value; return }
  goPage(jumpPage.value)
}

function onPageSizeChange() {
  currentPage.value = 1
  jumpPage.value = 1
  fetchWords()
}
function levelBadge(level) {
  if (level === 'CET-4') return 'badge-green'
  if (level === 'CET-6') return 'badge-blue'
  if (level === '考研') return 'badge-red'
  return 'badge-gray'
}

async function fetchTodayEntries() {
  const today = new Date().toISOString().slice(0, 10)
  try {
    const data = await planApi.getDailyWords(today)
    todayEntries.value = new Set((data.words || []).map(w => w.wordId))
  } catch {
    todayEntries.value = new Set()
  }
}

function planCount(wordId) {
  return todayEntries.value.has(wordId) ? 1 : 0
}

function inToday(wordId) {
  return todayEntries.value.has(wordId)
}

async function addToPlan(w) {
  const today = new Date().toISOString().slice(0, 10)
  try {
    await planApi.addEntry({ wordId: w.id, planDate: today })
    todayEntries.value.add(w.id)
    showToast(`已将「${w.word}」加入今天的学习计划`, 'success')
  } catch {
    showToast(`「${w.word}」已在今天的学习计划中`, 'warning')
  }
}

async function addAllToPlan() {
  const words = store.currentBookWords
  if (!words || !words.length) return
  const today = new Date().toISOString().slice(0, 10)
  const wordIds = words.map(w => w.id)
  try {
    await planApi.batchAddEntries({ wordIds, planDate: today })
    wordIds.forEach(id => todayEntries.value.add(id))
    showToast(`已将 ${wordIds.length} 个单词加入今天的学习计划`, 'success')
  } catch {
    showToast('批量加入失败，请重试', 'warning')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [activePlan] = await Promise.all([
      planApi.getActive(),
      store.fetchBooks(),
      store.fetchStrategies(),
      store.fetchPosCategories(),
      fetchTodayEntries(),
    ])
    boundBookId.value = activePlan?.wordBook?.id || ''
    if (store.books.length) {
      selectedBook.value = boundBookId.value || store.books[0].id
    }
  } finally {
    loading.value = false
  }
})

watch(selectedBook, () => {
  currentPage.value = 1
  fetchWords()
})
</script>

<style scoped>
.book-card {
  cursor: pointer; transition: .15s; text-align: center; padding: 24px;
  border: 2px solid transparent; position: relative;
}
.book-card:hover { border-color: var(--color-primary); transform: translateY(-2px); }
.book-active { border-color: var(--color-primary); background: var(--color-primary-light); }
.book-bound { border-color: var(--color-success); }
.book-name { font-size: 18px; font-weight: 600; margin-bottom: 6px; }
.book-level { margin-bottom: 6px; }
.book-count { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 4px; }
.book-desc { font-size: 13px; color: var(--color-text-secondary); }
.bound-tag {
  position: absolute; top: -8px; right: -8px;
  padding: 3px 10px; border-radius: 12px;
  font-size: 11px; font-weight: 700;
  background: var(--color-success); color: #fff;
}
.book-filter-bar { display: flex; flex-direction: column; gap: 4px; }
.filter-group { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.filter-group label { font-size: 13px; font-weight: 500; color: var(--color-text-secondary); min-width: 50px; }
.letter-btn { min-width: 36px; justify-content: center; font-size: 12px; padding: 4px 8px; }
.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.preview-header h3 { font-size: 15px; margin: 0; }
.preview-count { font-size: 13px; color: var(--color-text-secondary); }
.word-link { font-weight: 600; }
.toast {
  position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
  padding: 12px 24px; border-radius: var(--radius-sm); font-size: 14px;
  cursor: pointer; z-index: 999; box-shadow: var(--shadow-lg); animation: fadeIn .2s;
}
.toast-success { background: var(--color-success); color: #fff; }
.toast-warning { background: var(--color-warning); color: #fff; }
@keyframes fadeIn { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }
.plan-count-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px;
  font-size: 11px; font-weight: 700; border-radius: 9px;
  background: var(--color-primary); color: #fff;
  margin-left: 4px; vertical-align: super;
}
.action-cell { text-align: center; }
.btn-add {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid var(--color-primary); background: var(--color-primary-light);
  color: var(--color-primary); font-size: 18px; font-weight: 700;
  cursor: pointer; line-height: 1; display: inline-flex; align-items: center; justify-content: center;
  transition: .15s;
}
.btn-add:hover { background: var(--color-primary); color: #fff; }
.check-tag { font-size: 18px; color: var(--color-success); font-weight: 700; }
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 16px 0;
}
.page-info { font-size: 14px; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; }
.page-jump { width: 56px; text-align: center; padding: 4px 6px; }
.page-size { width: auto; padding: 4px 6px; }
@media (max-width: 768px) {
  .filter-group { gap: 6px; }
  .filter-group label { min-width: auto; margin-right: 4px; }
  .letter-btn { min-width: 32px; padding: 6px 6px; font-size: 11px; }
  .preview-header { flex-direction: column; align-items: flex-start; gap: 4px; }
  .action-cell { white-space: nowrap; }
}
</style>
