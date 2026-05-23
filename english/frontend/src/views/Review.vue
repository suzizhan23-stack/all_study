<template>
  <div class="review-page">
    <div class="review-header">
      <div class="review-mode-tabs">
        <button v-for="m in modes" :key="m.key" class="btn" :class="{ 'btn-primary': mode === m.key }" @click="mode = m.key">{{ m.label }}</button>
      </div>
      <div class="review-progress">进度 {{ currentIndex + 1 }}/{{ queue.length }} <span class="badge badge-green">{{ correctCount }} 正确</span> <span class="badge badge-red">{{ wrongCount }} 错误</span></div>
    </div>

    <div v-if="loading" class="card" style="text-align:center;padding:40px">加载中...</div>

    <template v-else-if="queue.length">
      <div class="card review-card">
        <template v-if="mode === 'card'">
          <div class="card-mode">
            <div v-if="!flipped" class="card-front" @click="flipped = true">
              <div class="card-word">{{ currentWord.word }}</div>
              <div class="card-hint">点击翻转</div>
            </div>
            <div v-else class="card-back" @click="flipped = false">
              <div class="card-word">{{ currentWord.word }}</div>
              <div class="card-phonetic phonetic">{{ currentWord.phonetic_uk }}</div>
              <div class="card-meaning">{{ currentWord.meaning_cn }}</div>
              <div class="card-hint">点击返回</div>
            </div>
          </div>
          <div class="card-actions" v-if="flipped">
            <button class="btn btn-danger btn-lg" @click="answer(false)">😢 忘记了</button>
            <button class="btn btn-warning btn-lg" @click="answer(false)">🤔 模糊</button>
            <button class="btn btn-success btn-lg" @click="answer(true)">😊 记住了</button>
          </div>
        </template>

        <template v-else-if="mode === 'choice'">
          <div class="quiz-mode">
            <div class="quiz-question">请选择 "{{ currentWord.word }}" 的中文释义：</div>
            <div class="quiz-options">
              <button v-for="(opt, i) in shuffledOptions" :key="i" class="btn quiz-option" :class="answered ? (opt === currentWord.meaning_cn ? 'correct' : 'wrong') : ''" :disabled="answered" @click="selectChoice(opt)">{{ opt }}</button>
            </div>
            <button v-if="answered" class="btn btn-primary" style="margin-top:16px" @click="nextCard">下一题</button>
          </div>
        </template>

        <template v-else-if="mode === 'spelling'">
          <div class="quiz-mode">
            <div class="quiz-question">拼写单词：</div>
            <div class="quiz-hint">{{ currentWord.meaning_cn }}  / {{ currentWord.phonetic_uk }}</div>
            <input v-model="spellingInput" class="input spelling-input" placeholder="输入英文..." :disabled="answered" @keyup.enter="checkSpelling" />
            <div v-if="answered" class="spelling-result" :class="spellingCorrect ? 'correct' : 'wrong'">
              {{ spellingCorrect ? '✅ 正确！' : '❌ 正确答案: ' + currentWord.word }}
            </div>
            <button v-if="answered" class="btn btn-primary" style="margin-top:12px" @click="nextCard">下一题</button>
            <button v-else class="btn btn-primary" style="margin-top:12px" @click="checkSpelling">确认</button>
          </div>
        </template>

        <template v-else>
          <div class="quiz-mode">
            <div class="quiz-question">听音辨义：</div>
            <button class="btn btn-lg" style="font-size:24px;padding:20px 40px;margin:20px 0">🔊 播放发音</button>
            <div class="quiz-options">
              <button v-for="(opt, i) in shuffledOptions" :key="i" class="btn quiz-option" :class="answered ? (opt === currentWord.meaning_cn ? 'correct' : 'wrong') : ''" :disabled="answered" @click="selectChoice(opt)">{{ opt }}</button>
            </div>
            <button v-if="answered" class="btn btn-primary" style="margin-top:16px" @click="nextCard">下一题</button>
          </div>
        </template>
      </div>

      <div class="card" style="margin-top:16px">
        <div class="review-stats">
          <span>本次复习: 已学 {{ currentIndex + (answered ? 1 : 0) }} 词</span>
          <span>正确 {{ correctCount }}</span>
          <span>错误 {{ wrongCount }}</span>
          <span>耗时 {{ elapsed }}m</span>
        </div>
      </div>
    </template>

    <div v-else class="card" style="text-align:center;padding:40px">
      <p>暂无待复习的单词</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { reviewApi } from '../api'

const modes = [
  { key: 'card', label: '卡牌' },
  { key: 'choice', label: '选择' },
  { key: 'spelling', label: '拼写' },
  { key: 'listening', label: '听力' },
]

const mode = ref('card')
const queue = ref([])
const currentIndex = ref(0)
const flipped = ref(false)
const answered = ref(false)
const correctCount = ref(0)
const wrongCount = ref(0)
const spellingInput = ref('')
const spellingCorrect = ref(false)
const selectedChoice = ref('')
const elapsed = ref(0)
const loading = ref(true)
const cardStartTime = ref(Date.now())

const currentWord = computed(() => queue.value[currentIndex.value] || {})

const shuffledOptions = computed(() => {
  if (!currentWord.value.meaning_cn) return []
  const all = [currentWord.value.meaning_cn, '保留', '坚持', '继续']
  return all.sort(() => Math.random() - 0.5)
})

onMounted(async () => {
  try {
    const data = await reviewApi.getQueue({ mode: mode.value, limit: 10 })
    queue.value = data.queue || []
  } catch {
    queue.value = []
  } finally {
    loading.value = false
  }
})

async function answer(correct) {
  const responseTimeMs = Date.now() - cardStartTime.value
  if (correct) correctCount.value++
  else wrongCount.value++
  answered.value = true
  try {
    await reviewApi.submitResult({
      wordId: currentWord.value.wordId || currentWord.value.id,
      quizType: mode.value,
      isCorrect: correct,
      responseTimeMs,
    })
  } catch {
    // silently fail
  }
}

function selectChoice(opt) {
  selectedChoice.value = opt
  answer(opt === currentWord.value.meaning_cn)
}

function checkSpelling() {
  spellingCorrect.value = spellingInput.value.trim().toLowerCase() === currentWord.value.word.toLowerCase()
  answer(spellingCorrect.value)
}

function nextCard() {
  if (currentIndex.value < queue.value.length - 1) {
    currentIndex.value++
  } else {
    currentIndex.value = 0
  }
  answered.value = false
  flipped.value = false
  spellingInput.value = ''
  spellingCorrect.value = false
  cardStartTime.value = Date.now()
}
</script>

<style scoped>
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.review-mode-tabs { display: flex; gap: 4px; }
.review-progress { font-size: 14px; color: var(--color-text-secondary); }
.review-card { text-align: center; padding: 40px; }
.card-mode { cursor: pointer; min-height: 200px; display: flex; flex-direction: column; justify-content: center; }
.card-word { font-size: 36px; font-weight: 700; margin-bottom: 12px; }
.card-phonetic { font-size: 18px; margin-bottom: 12px; }
.card-meaning { font-size: 20px; color: var(--color-text-secondary); }
.card-hint { font-size: 13px; color: var(--color-text-secondary); margin-top: 20px; }
.card-actions { display: flex; gap: 12px; justify-content: center; margin-top: 24px; }
.btn-lg { padding: 12px 32px; font-size: 16px; }
.btn-warning { background: var(--color-warning); color: #fff; border-color: var(--color-warning); }
.quiz-question { font-size: 18px; font-weight: 600; margin-bottom: 12px; }
.quiz-hint { font-size: 15px; color: var(--color-text-secondary); margin-bottom: 16px; }
.quiz-options { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; max-width: 500px; margin: 0 auto; }
.quiz-option { padding: 14px 20px; font-size: 15px; }
.quiz-option.correct { background: var(--color-success); color: #fff; border-color: var(--color-success); }
.quiz-option.wrong { background: var(--color-danger); color: #fff; border-color: var(--color-danger); }
.spelling-input { max-width: 300px; margin: 0 auto; text-align: center; font-size: 18px; }
.spelling-result { font-size: 18px; font-weight: 600; margin-top: 12px; }
.spelling-result.correct { color: var(--color-success); }
.spelling-result.wrong { color: var(--color-danger); }
.review-stats { display: flex; gap: 20px; justify-content: center; font-size: 14px; color: var(--color-text-secondary); }
</style>
