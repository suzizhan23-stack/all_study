<template>
  <div class="wrong-page">
    <h2 style="margin-bottom:16px">❌ 错题本</h2>

    <div class="card" style="margin-bottom:16px">
      <div class="wrong-stats">
        <div class="wrong-stat">
          <span class="wrong-stat-value">12</span>
          <span class="wrong-stat-label">近7天错题</span>
        </div>
        <div class="wrong-stat">
          <span class="wrong-stat-value">abandon</span>
          <span class="wrong-stat-label">最高错词(3次)</span>
        </div>
        <div class="wrong-stat">
          <span class="wrong-stat-value">拼写</span>
          <span class="wrong-stat-label">薄弱题型(7次)</span>
        </div>
      </div>
    </div>

    <div class="wrong-type-tabs" style="margin-bottom:12px">
      <button class="btn btn-primary btn-sm">全部</button>
      <button class="btn btn-sm">拼写</button>
      <button class="btn btn-sm">听力</button>
      <button class="btn btn-sm">释义</button>
    </div>

    <div v-for="w in wrongWords" :key="w.word_id" class="card wrong-item">
      <div class="wrong-head">
        <strong class="wrong-word">{{ w.word }}</strong>
        <span class="badge badge-red">错 {{ w.count }} 次</span>
        <router-link :to="`/learn`" class="btn btn-sm btn-primary">巩固复习</router-link>
      </div>
      <div v-for="log in w.logs" :key="log.time" class="wrong-log">
        <span class="badge" :class="log.type === '拼写' ? 'badge-gray' : 'badge-blue'">{{ log.type }}</span>
        <span v-if="log.answer" class="wrong-answer">"{{ log.answer }}"</span>
        <span class="wrong-time">{{ log.time }}</span>
      </div>
    </div>

    <div class="card" style="margin-top:16px;text-align:center;padding:20px">
      <router-link to="/learn" class="btn btn-primary">一键复习全部错题</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const wrongWords = ref([
  {
    word_id: 'w1', word: 'abandon', count: 3,
    logs: [
      { type: '拼写', answer: 'abanden', time: '2026-05-20' },
      { type: '释义', answer: '丢弃', time: '2026-05-19' },
    ],
  },
  {
    word_id: 'w2', word: 'remarkable', count: 2,
    logs: [
      { type: '拼写', answer: 'remarkble', time: '2026-05-18' },
    ],
  },
])
</script>

<style scoped>
.wrong-stats { display: flex; gap: 24px; justify-content: center; }
.wrong-stat { text-align: center; }
.wrong-stat-value { display: block; font-size: 20px; font-weight: 700; }
.wrong-stat-label { font-size: 13px; color: var(--color-text-secondary); }
.wrong-type-tabs { display: flex; gap: 4px; }
.wrong-item { margin-bottom: 12px; }
.wrong-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.wrong-word { font-size: 16px; }
.wrong-log { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 14px; }
.wrong-answer { color: var(--color-danger); }
.wrong-time { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }
</style>
