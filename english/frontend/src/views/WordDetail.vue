<template>
  <div class="word-detail" v-if="loading">
    <div class="card" style="text-align:center;padding:40px">加载中...</div>
  </div>
  <div class="word-detail" v-else-if="error">
    <div class="card" style="text-align:center;padding:40px;color:var(--color-danger)">{{ error }}</div>
  </div>
  <div class="word-detail" v-else-if="word">
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
      <div class="freq-label">个人频率</div>
      <div class="freq-slider">
        <input type="range" min="0" max="100" v-model.number="personalFreq" />
        <span class="freq-value">{{ personalFreq }}</span>
        <span style="font-size:13px;color:var(--color-text-secondary)">默认 {{ word.frequency }}</span>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>词源</h3>
      <p>{{ word.etymologyCn }}</p>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>详细释义</h3>
      <div v-for="(d, i) in definitions" :key="d.id" class="def-item">
        <span class="def-num">{{ i + 1 }}</span>
        <div>
          <div class="def-en">{{ d.meaningEn }}</div>
          <div class="def-cn">{{ d.meaningCn }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>固定搭配</h3>
      <div class="phrase-grid">
        <div v-for="c in collocations" :key="c.id" class="phrase-item">
          <div>
            <strong>{{ c.collocation }}</strong>
            <span class="list-trans">{{ c.translation }}</span>
          </div>
          <span class="badge badge-gray">freq {{ c.frequency }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>介词模式</h3>
      <div class="phrase-grid">
        <div v-for="p in prepPatterns" :key="p.id" class="phrase-item">
          <div>
            <strong>{{ p.pattern }}</strong>
            <span class="list-trans">{{ p.translation }}</span>
          </div>
          <span class="badge badge-blue">{{ p.preposition }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>例句</h3>
      <div v-for="e in examples" :key="e.id" class="example-item">
        <div class="example-en">{{ e.sentenceEn }}</div>
        <div class="example-cn">{{ e.sentenceCn }}</div>
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
          <span v-for="s in relations.synonyms" :key="s" class="syn-word">{{ s }}</span>
        </div>
        <div>
          <span class="syn-label">反义:</span>
          <span v-for="a in relations.antonyms" :key="a" class="syn-word">{{ a }}</span>
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
        <span v-for="t in myTags" :key="t" class="tag" :style="{ background: t.color + '20', color: t.color }">
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useWordStore } from '../stores/words'

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
const note = ref('')
const notePublic = ref(true)
const myTags = ref([])

onMounted(async () => {
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
      isFavorited.value = (ud.favorites && ud.favorites.length > 0) ?? false
      note.value = ud.notes?.content ?? ''
      notePublic.value = ud.notes?.isPublic ?? true
      myTags.value = ud.tags || []
    }
  } catch (e) {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
})

function saveNote() {
  if (store.currentWord) {
    store.updateNote(route.params.id, note.value, !notePublic.value)
  }
}
</script>

<style scoped>
.word-title { font-size: 32px; font-weight: 700; margin-bottom: 4px; }
.word-meta { font-size: 15px; color: var(--color-text-secondary); }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; }
.header-actions { display: flex; gap: 8px; }
.detail-badges { display: flex; gap: 8px; margin-top: 12px; }
.freq-label { font-size: 14px; font-weight: 500; margin-bottom: 8px; }
.freq-value { font-weight: 700; color: var(--color-primary); min-width: 28px; }
h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
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
  font-size: 13px;
}
.example-item { padding: 12px 0; border-bottom: 1px solid var(--color-border); }
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
</style>
