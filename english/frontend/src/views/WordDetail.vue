<template>
  <div class="word-detail" v-if="loading">
    <div class="card" style="text-align:center;padding:40px">加载中...</div>
  </div>
  <div class="word-detail" v-else-if="error">
    <div class="card" style="text-align:center;padding:40px;color:var(--color-danger)">{{ error }}</div>
  </div>
  <div class="word-detail" v-else-if="word">
    <button class="btn btn-sm back-btn" @click="$router.back()">← 返回</button>
    <div class="detail-header">
      <div>
        <h1 class="word-title">{{ word.word }}</h1>
        <div class="word-meta">
          <span class="phonetic">UK {{ word.phoneticUk }}</span>
          <span class="phonetic" style="margin-left:12px">US {{ word.phoneticUs }}</span>
          <button class="btn btn-sm" style="margin-left:8px">🔊 UK</button>
          <button class="btn btn-sm">🔊 US</button>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn" :class="{ 'btn-primary': isKeyPoint }" @click="toggleKeyPoint">{{ isKeyPoint ? '⭐' : '☆' }} 今日重点</button>
        <button class="btn" :class="{ 'btn-primary': isFavorited }">⭐ 收藏</button>
        <button class="btn">📝 笔记</button>
      </div>
    </div>

    <div class="detail-badges">
      <span class="badge badge-blue">{{ word.pos }}</span>
      <span class="badge badge-gray">{{ word.source }}</span>
      <span class="badge badge-green">难度 {{ '⭐'.repeat(word.difficulty) }}</span>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="freq-bar" :title="'频率 ' + personalFreq"><div class="freq-fill" :style="{ width: personalFreq + '%' }"></div></div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>词源与释义</h3>
      <p v-if="word.etymologyCn" class="etymology-text">{{ word.etymologyCn }}</p>
      <div v-for="(d, i) in definitions" :key="d.id" class="def-item">
        <span class="def-num">{{ i + 1 }}</span>
        <div>
          <div class="def-en">{{ d.meaningEn }}</div>
          <div class="def-cn">{{ d.meaningCn }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>固定搭配 <span class="section-count">{{ collocations.length }}</span></h3>
      <div class="phrase-grid">
        <div v-for="c in collocations" :key="c.id" class="phrase-item" :class="{ 'phrase-revealed': revealedIds.has('c-' + c.id), 'phrase-menu-open': activeMenu === 'c-' + c.id }"
          @click="toggleReveal('c-' + c.id)"
          @mousedown="startLongPress('c', c.id)" @mouseup="cancelLongPress" @mouseleave="cancelLongPress"
          @touchstart="startLongPress('c', c.id)" @touchend="cancelLongPress" @touchmove="cancelLongPress">
          <div>
            <strong>{{ c.collocation }}</strong>
            <span v-if="revealedIds.has('c-' + c.id)" class="list-trans">{{ c.translation }}</span>
          </div>
          <div v-if="isAdmin && activeMenu === 'c-' + c.id" class="phrase-actions" @click.stop>
            <button class="btn btn-xs" @click="editCollocation(c)">✏️</button>
            <button class="btn btn-xs btn-danger" @click="deleteCollocation(c)">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>介词模式 <span class="section-count">{{ prepPatterns.length }}</span></h3>
      <div class="phrase-grid">
        <div v-for="p in prepPatterns" :key="p.id" class="phrase-item" :class="{ 'phrase-revealed': revealedIds.has('p-' + p.id), 'phrase-menu-open': activeMenu === 'p-' + p.id }"
          @click="toggleReveal('p-' + p.id)"
          @mousedown="startLongPress('p', p.id)" @mouseup="cancelLongPress" @mouseleave="cancelLongPress"
          @touchstart="startLongPress('p', p.id)" @touchend="cancelLongPress" @touchmove="cancelLongPress">
          <div>
            <strong>{{ p.pattern }}</strong>
            <span v-if="revealedIds.has('p-' + p.id)" class="list-trans">{{ p.translation }}</span>
          </div>
          <span class="badge badge-blue">{{ p.preposition }}</span>
          <div v-if="isAdmin && activeMenu === 'p-' + p.id" class="phrase-actions" @click.stop>
            <button class="btn btn-xs" @click="editPrepPattern(p)">✏️</button>
            <button class="btn btn-xs btn-danger" @click="deletePrepPattern(p)">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>例句 <span class="section-count">{{ examples.length }}</span></h3>
      <div v-for="e in examples" :key="e.id" class="example-item example-toggle" :class="{ 'example-revealed': revealedIds.has('e-' + e.id) }" @click="toggleReveal('e-' + e.id)">
        <div v-if="!revealedIds.has('e-' + e.id)" class="example-en">{{ e.sentenceEn }}</div>
        <div v-else class="example-cn">{{ e.sentenceCn }}</div>
        <div class="example-meta">
          <span v-if="e.sourceDetail" class="badge badge-gray">{{ e.sourceDetail }}</span>
          <span>👍 {{ e.frequency }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>同反义词</h3>
      <div class="syn-row">
        <div>
          <span class="syn-label">同义:</span>
          <span v-for="s in relations.synonyms" :key="s.wordId" class="syn-word">{{ s.word }}</span>
        </div>
        <div>
          <span class="syn-label">反义:</span>
          <span v-for="a in relations.antonyms" :key="a.wordId" class="syn-word">{{ a.word }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>我的笔记</h3>
      <textarea v-model="note" class="input note-input" placeholder="添加你的笔记..." rows="3"></textarea>
      <div class="note-actions">
        <label><input type="checkbox" v-model="notePublic" /> 公开</label>
        <button class="btn btn-sm btn-primary" @click="saveNote">保存笔记</button>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>我的标签</h3>
      <div class="tags-wrap">
        <span v-for="t in myTags" :key="t.id" class="tag" :style="{ background: (t.color || '#888') + '20', color: t.color || '#888' }">
          #{{ t.tag }}
        </span>
        <select class="input" style="width:auto;display:inline-block;margin-left:8px">
          <option value="">+ 添加标签</option>
          <option>写作词汇</option>
          <option>口语词汇</option>
          <option>考试必备</option>
        </select>
      </div>
    </div>

    <div class="detail-footer" style="margin-top:20px">
      <router-link to="/review" class="btn btn-success">🎴 开始学习这个词</router-link>
      <button class="btn">❌ 报错</button>
      <button class="btn">👍 有用</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useWordStore } from '../stores/words'
import { adminApi, planApi } from '../api'

const route = useRoute()
const store = useWordStore()

const loading = ref(true)
const error = ref(null)

const word = computed(() => store.currentWord || null)
const definitions = computed(() => store.currentWord?.definitions || [])
const collocations = computed(() => store.currentWord?.collocations || [])
const prepPatterns = computed(() => store.currentWord?.prepPatterns || [])
const examples = computed(() => store.currentWord?.examples || [])
const relations = computed(() => store.currentWord?.relations || { synonyms: [], antonyms: [] })

const personalFreq = ref(50)
const isFavorited = ref(false)
const isKeyPoint = ref(false)
const note = ref('')
const notePublic = ref(true)
const myTags = ref([])
const revealedIds = ref(new Set())
const isAdmin = ref(false)
const activeMenu = ref(null)
const justOpened = ref(false)
let longPressTimer = null

function toggleReveal(id) {
  if (justOpened.value) { justOpened.value = false; return }
  const s = revealedIds.value
  if (s.has(id)) s.delete(id); else s.add(id)
  revealedIds.value = new Set(s)
}

function startLongPress(type, id) {
  cancelLongPress()
  longPressTimer = setTimeout(() => {
    activeMenu.value = type + '-' + id
    justOpened.value = true
  }, 600)
}

function cancelLongPress() {
  clearTimeout(longPressTimer)
  longPressTimer = null
}

function closeMenu(e) {
  if (e.target.closest('.phrase-item')) return
  activeMenu.value = null
}

async function toggleKeyPoint() {
  if (!word.value?.id) return
  try {
    await planApi.toggleKeyPointByWord(word.value.id)
    isKeyPoint.value = !isKeyPoint.value
  } catch {}
}

async function editCollocation(c) {
  activeMenu.value = null
  const collocation = prompt('搭配：', c.collocation)
  if (!collocation) return
  const translation = prompt('翻译：', c.translation)
  if (!translation) return
  try {
    await adminApi.updateCollocation(c.id, { collocation, translation })
    if (store.currentWord) {
      const item = store.currentWord.collocations.find(x => x.id === c.id)
      if (item) { item.collocation = collocation; item.translation = translation }
    }
  } catch {}
}

async function deleteCollocation(c) {
  activeMenu.value = null
  if (!confirm(`确定删除搭配「${c.collocation}」？`)) return
  try {
    await adminApi.deleteCollocation(c.id)
    if (store.currentWord) {
      store.currentWord.collocations = store.currentWord.collocations.filter(x => x.id !== c.id)
    }
  } catch {}
}

async function editPrepPattern(p) {
  activeMenu.value = null
  const pattern = prompt('模式：', p.pattern)
  if (!pattern) return
  const translation = prompt('翻译：', p.translation)
  if (!translation) return
  try {
    await adminApi.updatePrepPattern(p.id, { pattern, translation })
    if (store.currentWord) {
      const item = store.currentWord.prepPatterns.find(x => x.id === p.id)
      if (item) { item.pattern = pattern; item.translation = translation }
    }
  } catch {}
}

async function deletePrepPattern(p) {
  activeMenu.value = null
  if (!confirm(`确定删除介词模式「${p.pattern}」？`)) return
  try {
    await adminApi.deletePrepPattern(p.id)
    if (store.currentWord) {
      store.currentWord.prepPatterns = store.currentWord.prepPatterns.filter(x => x.id !== p.id)
    }
  } catch {}
}

function getJwtPayload() {
  try { return JSON.parse(atob((localStorage.getItem('auth_token') || '').split('.')[1])) } catch { return null }
}

onMounted(async () => {
  isAdmin.value = getJwtPayload()?.role === 'admin'
  document.addEventListener('click', closeMenu)
  const id = route.params.id
  if (!id) {
    error.value = '缺少单词 ID'
    loading.value = false
    return
  }
  try {
    await store.fetchWordDetail(id)
    if (store.currentWord) {
      const ud = store.currentWord.userData || {}
      personalFreq.value = ud.frequency ?? 50
      isFavorited.value = (ud.favorites?.length ?? 0) > 0
      isKeyPoint.value = ud.isKeyPoint ?? ud.keyPoint ?? false
      note.value = ud.notes?.content ?? ''
      notePublic.value = ud.notes ? !ud.notes.private : true
      myTags.value = ud.tags || []
    }
  } catch (e) {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenu)
})

function saveNote() {
  if (store.currentWord) {
    store.updateNote(route.params.id, note.value, !notePublic.value)
  }
}
</script>

<style scoped>
.back-btn { margin-bottom: 12px; }
.word-title { font-size: 32px; font-weight: 700; margin-bottom: 4px; }
.word-meta { font-size: 15px; color: var(--color-text-secondary); }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; }
.header-actions { display: flex; gap: 8px; }
.detail-badges { display: flex; gap: 8px; margin-top: 12px; }
.freq-bar { height: 8px; background: var(--color-border); border-radius: 4px; overflow: hidden; }
.freq-fill { height: 100%; background: var(--color-primary); border-radius: 4px; transition: width .3s; }
.etymology-text { font-size: 15px; color: var(--color-text-secondary); margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border); }
h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.section-count { font-size: 13px; color: var(--color-text-secondary); font-weight: 400; }
.def-item { display: flex; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--color-border); }
.def-item:last-child { border-bottom: none; }
.def-num { width: 24px; height: 24px; border-radius: 50%; background: var(--color-primary-light); color: var(--color-primary); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0; }
.def-en { font-size: 15px; margin-bottom: 2px; }
.def-cn { font-size: 14px; color: var(--color-text-secondary); }
.list-trans { display: block; font-size: 13px; color: var(--color-text-secondary); white-space: nowrap; }
.phrase-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.phrase-item {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 6px 12px; border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); background: var(--color-bg);
  font-size: 13px; cursor: pointer; transition: .15s;
}
.phrase-item:hover { border-color: var(--color-primary); }
.phrase-toggle { cursor: pointer; transition: .15s; }
.phrase-toggle:hover { border-color: var(--color-primary); }
.phrase-revealed { border-color: var(--color-primary); background: var(--color-primary-light); }
.phrase-menu-open { border-color: var(--color-danger) !important; background: #fef2f2 !important; }
.phrase-actions { display: flex; gap: 4px; margin-left: 4px; }
.btn-xs { padding: 2px 6px; font-size: 12px; line-height: 1; }
.btn-danger { color: var(--color-danger); border-color: var(--color-danger); }
.btn-danger:hover { background: var(--color-danger); color: #fff; }
.example-item { padding: 12px 0; border-bottom: 1px solid var(--color-border); }
.example-toggle { cursor: pointer; transition: background .15s; padding: 12px 16px; margin: 0 -16px; border-radius: var(--radius-sm); }
.example-toggle:hover { background: var(--color-bg); }
.example-revealed { background: var(--color-primary-light) !important; }
.example-item:last-child { border-bottom: none; }
.example-en { font-size: 15px; margin-bottom: 4px; }
.example-cn { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 6px; }
.example-meta { display: flex; gap: 10px; font-size: 13px; color: var(--color-text-secondary); }
.syn-row { display: flex; flex-direction: column; gap: 8px; }
.syn-label { font-weight: 600; margin-right: 8px; }
.syn-word { display: inline-block; padding: 2px 10px; background: var(--color-primary-light); border-radius: 6px; margin: 2px 4px; font-size: 13px; }
.note-input { margin-top: 8px; resize: vertical; }
.note-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.tags-wrap { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.detail-footer { display: flex; gap: 10px; }
@media (max-width: 768px) {
  .word-title { font-size: 24px; }
  .detail-header { flex-direction: column; gap: 12px; }
  .header-actions { width: 100%; }
  .header-actions .btn { flex: 1; justify-content: center; }
  .detail-badges { flex-wrap: wrap; }
}
</style>
