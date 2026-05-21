<template>
  <div class="folder-detail">
    <div class="folder-detail-header">
      <router-link to="/favorites" class="btn btn-sm">← 返回收藏夹</router-link>
      <h2>{{ folder.name }}</h2>
      <div>
        <button class="btn btn-sm">编辑</button>
        <button class="btn btn-sm">🔗 分享</button>
      </div>
    </div>
    <div class="folder-meta">
      <span class="badge badge-blue">{{ folder.category }}</span>
      <span>{{ items.length }} 个条目</span>
    </div>
    <div class="folder-actions-bar">
      <button class="btn btn-sm btn-danger">批量删除</button>
      <button class="btn btn-sm">批量加标签</button>
    </div>
    <table class="table" style="margin-top:12px">
      <thead>
        <tr>
          <th style="width:30px"><input type="checkbox" /></th>
          <th>单词</th>
          <th>释义</th>
          <th>频率</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td><input type="checkbox" /></td>
          <td><router-link :to="`/word/${item.entity_id}`" style="font-weight:600">{{ item.word }}</router-link></td>
          <td>{{ item.meaning }}</td>
          <td><span class="badge badge-gray">freq {{ item.freq }}</span></td>
          <td>
            <button class="btn btn-sm">📝</button>
            <button class="btn btn-sm">🏷️</button>
            <button class="btn btn-sm btn-danger" @click="removeItem(item.id)">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const folders = { f1: { id: 'f1', name: '稍后复习', category: 'word' } }
const folder = ref(folders['f1'])

const items = ref([
  { id: 'i1', entity_id: 'w1', word: 'abandon', meaning: '放弃；遗弃；抛弃', freq: 72 },
  { id: 'i2', entity_id: 'w2', word: 'remarkable', meaning: '显著的', freq: 50 },
  { id: 'i3', entity_id: 'w3', word: 'contribute', meaning: '贡献', freq: 30 },
])

function removeItem(id) {
  items.value = items.value.filter(i => i.id !== id)
}
</script>

<style scoped>
.folder-detail-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
.folder-detail-header h2 { flex: 1; font-size: 20px; }
.folder-meta { display: flex; gap: 12px; font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.folder-actions-bar { display: flex; gap: 8px; }
</style>
