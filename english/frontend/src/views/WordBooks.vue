<template>
  <div class="wordbooks-page">
    <h2 style="margin-bottom:16px">📚 单词本</h2>

    <!-- 单词本列表 -->
    <div class="grid-3" style="margin-bottom:24px">
      <div
        v-for="b in store.books"
        :key="b.id"
        class="card book-card"
        :class="{ 'book-active': selectedBook === b.id }"
        @click="selectedBook = b.id"
      >
        <div class="book-name">{{ b.name }}</div>
        <div class="book-level badge" :class="levelBadge(b.difficulty_level)">{{ b.difficulty_level }}</div>
        <div class="book-count">{{ b.word_count }} 词</div>
        <div class="book-desc">{{ b.description }}</div>
      </div>
    </div>

    <!-- 当前选中的单词本 -->
    <template v-if="currentBook">
      <!-- 开始学习面板：选择策略 + 每日数量 -->
      <div class="card" style="margin-bottom:16px">
        <h3>🎯 开始学习</h3>
        <div class="start-panel">
          <div class="start-row">
            <label>学习策略：</label>
            <div class="strategy-options">
              <div
                v-for="s in store.strategies"
                :key="s.id"
                class="strategy-card"
                :class="{ 'strategy-selected': selectedStrategy === s.id }"
                @click="selectedStrategy = s.id"
              >
                <div class="strategy-name">{{ s.name }}</div>
                <div class="strategy-desc">{{ s.description }}</div>
              </div>
            </div>
          </div>
          <div class="start-row" style="align-items:center">
            <label>每日词数：</label>
            <input v-model.number="dailyCount" class="input" type="number" min="5" max="100" style="max-width:100px" />
            <span style="font-size:14px;color:var(--color-text-secondary);margin-left:8px">词 / 天</span>
            <button class="btn btn-success btn-lg" style="margin-left:auto" @click="startLearning">🚀 开始学习</button>
          </div>
          <div v-if="currentStrategy" class="strategy-hint">
            <strong>当前策略预览：</strong>
            <span>{{ strategyPreview }}</span>
          </div>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="card" style="margin-bottom:16px">
        <div class="book-filter-bar">
          <div class="filter-group">
            <label>词性：</label>
            <button
              v-for="cat in store.posCategories"
              :key="cat"
              class="btn btn-sm"
              :class="{ 'btn-primary': selectedPos === cat }"
              @click="togglePos(cat)"
            >{{ cat }}</button>
            <button v-if="selectedPos" class="btn btn-sm" @click="selectedPos = ''">清除</button>
          </div>
          <div class="filter-group" style="margin-top:8px">
            <label>首字母：</label>
            <button
              v-for="l in store.letters"
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
          <span class="preview-count">{{ filteredWords.length }} 个词</span>
        </div>
        <table class="table">
          <thead>
            <tr><th>#</th><th>单词</th><th>词性</th><th>释义</th><th>首字母</th><th>难度</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="(w, i) in filteredWords" :key="w.id">
              <td>{{ i + 1 }}</td>
              <td>
                <router-link :to="`/word/${w.id}`" class="word-link">{{ w.word }}</router-link>
                <span v-if="planCount(w.word) > 0" class="plan-count-badge" :title="`已加入 ${planCount(w.word)} 次`">{{ planCount(w.word) }}</span>
              </td>
              <td><span class="badge badge-blue">{{ w.pos }}</span></td>
              <td>{{ w.meaning_cn }}</td>
              <td><span class="badge badge-gray">{{ w.first_letter }}</span></td>
              <td>{{ '⭐'.repeat(w.difficulty) }}</td>
              <td class="action-cell">
                <button v-if="!inToday(w.word)" class="btn-add" title="加入学习计划" @click="addToPlan(w.word)">+</button>
                <span v-else class="check-tag">✓</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filteredWords.length" class="empty-state" style="padding:40px">
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useWordBookStore } from '../stores/wordBooks'
import { useDailyPlanStore } from '../stores/dailyPlan'

const router = useRouter()
const store = useWordBookStore()
const planStore = useDailyPlanStore()

const selectedBook = ref('wb1')
const selectedPos = ref('')
const selectedLetter = ref('')
const selectedStrategy = ref('s1')
const dailyCount = ref(10)

const toast = ref({ show: false, msg: '', type: 'success' })
let toastTimer = null

function showToast(msg, type = 'success') {
  toast.value = { show: true, msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 2000)
}

const currentBook = computed(() => store.books.find(b => b.id === selectedBook.value))
const currentStrategy = computed(() => store.strategies.find(s => s.id === selectedStrategy.value))

const sampleData = [
  { word: 'abandon', pos: 'vt.', meaning: '放弃', letter: 'A', diff: 2 },
  { word: 'remarkable', pos: 'adj.', meaning: '显著的', letter: 'R', diff: 3 },
  { word: 'contribute', pos: 'v.', meaning: '贡献', letter: 'C', diff: 2 },
  { word: 'significant', pos: 'adj.', meaning: '重要的', letter: 'S', diff: 3 },
  { word: 'approach', pos: 'v.', meaning: '接近', letter: 'A', diff: 1 },
  { word: 'demonstrate', pos: 'vt.', meaning: '证明', letter: 'D', diff: 3 },
  { word: 'establish', pos: 'vt.', meaning: '建立', letter: 'E', diff: 2 },
  { word: 'fundamental', pos: 'adj.', meaning: '基本的', letter: 'F', diff: 3 },
  { word: 'generate', pos: 'vt.', meaning: '产生', letter: 'G', diff: 2 },
  { word: 'hypothesis', pos: 'n.', meaning: '假设', letter: 'H', diff: 4 },
]

const allPreviewWords = computed(() => {
  return Array.from({ length: 30 }, (_, i) => {
    const s = sampleData[i % sampleData.length]
    return { id: `preview-${i}`, ...s }
  })
})

const filteredWords = computed(() => {
  let result = allPreviewWords.value
  if (selectedPos.value) {
    const posPatterns = store.posMap[selectedPos.value] || []
    result = result.filter(w => posPatterns.includes(w.pos))
  }
  if (selectedLetter.value) {
    result = result.filter(w => w.first_letter === selectedLetter.value)
  }
  return result
})

const strategyPreview = computed(() => {
  if (!currentStrategy.value) return ''
  const type = currentStrategy.value.type
  const parts = [`从「${currentBook.value.name}」中`]
  if (type === 'random') parts.push('完全随机抽取')
  else if (type === 'alphabetical') parts.push('按字母 A→Z 顺序抽取')
  else if (type === 'pos_alphabetical') parts.push('按选定词性 + 字母顺序抽取')
  else if (type === 'pos_random') parts.push('按选定词性 + 随机抽取')
  else if (type === 'difficulty_asc') parts.push('从简单到困难抽取')
  else if (type === 'difficulty_desc') parts.push('从困难到简单抽取')
  parts.push(`，每日 ${dailyCount.value} 词`)
  return parts.join('')
})

function togglePos(cat) {
  selectedPos.value = selectedPos.value === cat ? '' : cat
}
function toggleLetter(l) {
  selectedLetter.value = selectedLetter.value === l ? '' : l
}
function levelBadge(level) {
  if (level === 'CET-4') return 'badge-green'
  if (level === 'CET-6') return 'badge-blue'
  if (level === '考研') return 'badge-red'
  return 'badge-gray'
}
function startLearning() {
  router.push('/review')
}
function planCount(word) {
  return planStore.wordPlanCount(word)
}
function inToday(word) {
  return planStore.isInTodayPlan(word)
}
function addToPlan(word) {
  const ok = planStore.addWord(word)
  if (ok) showToast(`已将「${word}」加入今天的学习计划`, 'success')
  else showToast(`「${word}」已在今天的学习计划中`, 'warning')
}
</script>

<style scoped>
.book-card {
  cursor: pointer; transition: .15s; text-align: center; padding: 24px;
  border: 2px solid transparent;
}
.book-card:hover { border-color: var(--color-primary); transform: translateY(-2px); }
.book-active { border-color: var(--color-primary); background: var(--color-primary-light); }
.book-name { font-size: 18px; font-weight: 600; margin-bottom: 6px; }
.book-level { margin-bottom: 6px; }
.book-count { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 4px; }
.book-desc { font-size: 13px; color: var(--color-text-secondary); }
.book-filter-bar { display: flex; flex-direction: column; gap: 4px; }
.filter-group { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.filter-group label { font-size: 13px; font-weight: 500; color: var(--color-text-secondary); min-width: 50px; }
.letter-btn { min-width: 36px; justify-content: center; font-size: 12px; padding: 4px 8px; }
.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.preview-header h3 { font-size: 15px; margin: 0; }
.preview-count { font-size: 13px; color: var(--color-text-secondary); }
.word-link { font-weight: 600; }
.start-panel { display: flex; flex-direction: column; gap: 16px; margin-top: 12px; }
.start-row { display: flex; gap: 12px; }
.start-row > label { min-width: 80px; font-size: 14px; font-weight: 500; padding-top: 6px; }
.strategy-options { display: flex; flex-wrap: wrap; gap: 8px; }
.strategy-card {
  padding: 12px 16px; border: 1px solid var(--color-border); border-radius: var(--radius-sm);
  cursor: pointer; transition: .15s; min-width: 140px;
}
.strategy-card:hover { border-color: var(--color-primary); }
.strategy-selected { border-color: var(--color-primary); background: var(--color-primary-light); }
.strategy-name { font-size: 14px; font-weight: 600; }
.strategy-desc { font-size: 12px; color: var(--color-text-secondary); }
.strategy-hint { font-size: 13px; color: var(--color-text-secondary); padding: 8px 12px; background: var(--color-bg); border-radius: var(--radius-sm); }
.btn-lg { padding: 12px 32px; font-size: 16px; }
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
</style>
