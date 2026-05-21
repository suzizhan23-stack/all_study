<template>
  <div class="plans-page">
    <h2 style="margin-bottom:16px">📋 学习计划</h2>

    <div v-if="activePlan" class="card active-plan" style="margin-bottom:20px">
      <h3>进行中</h3>
      <div class="plan-header">
        <div>
          <div class="plan-name">{{ activePlan.name }}</div>
          <div class="plan-sub">第 {{ activePlan.current_day }} 天 / 共 {{ activePlan.duration }} 天</div>
          <div class="progress-bar" style="margin-top:8px;max-width:400px">
            <div class="progress-bar-fill" :style="{ width: activePlan.pct + '%' }"></div>
          </div>
          <div class="plan-today" style="margin-top:8px">
            今日任务: {{ activePlan.daily }} 词 | 已完成 {{ activePlan.done }} 词
          </div>
        </div>
        <router-link to="/learn" class="btn btn-primary">继续学习 →</router-link>
      </div>
    </div>

    <h3 style="margin-bottom:12px">可选计划</h3>
    <div class="grid-3">
      <div v-for="p in availablePlans" :key="p.id" class="card plan-card">
        <div class="plan-card-name">{{ p.name }}</div>
        <div class="plan-card-detail">{{ p.duration }} 天 · 每日 {{ p.daily }} 词</div>
        <div class="plan-card-target" v-if="p.target">{{ p.target }}</div>
        <button class="btn btn-primary btn-sm" style="margin-top:12px" @click="joinPlan(p.id)">加入</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activePlan = ref({
  id: 'p1', name: 'CET-4 30天冲刺', current_day: 3, duration: 30,
  pct: 10, daily: 20, done: 12,
})

const availablePlans = ref([
  { id: 'p2', name: '考研词汇 60天', duration: 60, daily: 15, target: '考研' },
  { id: 'p3', name: '雅思7分词汇', duration: 45, daily: 25, target: '雅思7.0' },
  { id: 'p4', name: 'GRE 核心词汇', duration: 90, daily: 30, target: 'GRE' },
])

function joinPlan(id) {
  const plan = availablePlans.value.find(p => p.id === id)
  if (plan) {
    activePlan.value = {
      id: plan.id, name: plan.name, current_day: 1,
      duration: plan.duration, pct: 3, daily: plan.daily, done: 0,
    }
    availablePlans.value = availablePlans.value.filter(p => p.id !== id)
  }
}
</script>

<style scoped>
.active-plan { border: 2px solid var(--color-primary); }
.plan-header { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 12px; }
.plan-name { font-size: 18px; font-weight: 600; }
.plan-sub { font-size: 14px; color: var(--color-text-secondary); }
.plan-today { font-size: 14px; }
.plan-card { padding: 20px; }
.plan-card-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.plan-card-detail { font-size: 14px; color: var(--color-text-secondary); }
.plan-card-target { display: inline-block; font-size: 12px; padding: 2px 8px; background: var(--color-primary-light); color: var(--color-primary); border-radius: 4px; margin-top: 4px; }
</style>
