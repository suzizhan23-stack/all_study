<template>
  <div class="favorites-page">
    <div class="fav-header">
      <h2>📁 我的收藏夹</h2>
      <button class="btn btn-primary" @click="showNewFolder = true">+ 新建收藏夹</button>
    </div>

    <div v-if="showNewFolder" class="card" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;align-items:center">
        <input v-model="newFolderName" class="input" placeholder="收藏夹名称" style="max-width:300px" />
        <select v-model="newFolderCategory" class="input" style="max-width:150px">
          <option value="word">单词</option>
          <option value="example">例句</option>
          <option value="phrase">短语</option>
          <option value="article">文章</option>
          <option value="other">其他</option>
        </select>
        <button class="btn btn-primary btn-sm" @click="createFolder">创建</button>
        <button class="btn btn-sm" @click="showNewFolder = false">取消</button>
      </div>
    </div>

    <div class="grid-3">
      <div v-for="f in folders" :key="f.id" class="card folder-card">
        <div class="folder-icon">{{ folderIcon(f.category) }}</div>
        <div class="folder-name">{{ f.name }}</div>
        <div class="folder-category badge badge-blue">{{ f.category }}</div>
        <div class="folder-count">{{ f.count }} 个条目</div>
        <div v-if="f.is_default" class="folder-default">默认</div>
        <div class="folder-actions">
          <router-link :to="`/favorites/${f.id}`" class="btn btn-sm btn-primary">查看</router-link>
          <button v-if="!f.is_default" class="btn btn-sm" @click="deleteFolder(f.id)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderCategory = ref('word')

const folders = ref([
  { id: 'f1', name: '稍后复习', category: 'word', count: 12, is_default: true },
  { id: 'f2', name: '精彩例句', category: 'example', count: 5, is_default: false },
  { id: 'f3', name: '地道搭配', category: 'phrase', count: 8, is_default: false },
  { id: 'f4', name: '好文收藏', category: 'article', count: 3, is_default: false },
])

function folderIcon(cat) {
  return { word: '📖', example: '💬', phrase: '🔗', article: '📰', other: '📁' }[cat] || '📁'
}

function createFolder() {
  if (!newFolderName.value.trim()) return
  folders.value.push({
    id: 'f' + Date.now(),
    name: newFolderName.value,
    category: newFolderCategory.value,
    count: 0,
    is_default: false,
  })
  newFolderName.value = ''
  showNewFolder.value = false
}

function deleteFolder(id) {
  folders.value = folders.value.filter(f => f.id !== id)
}
</script>

<style scoped>
.fav-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.fav-header h2 { font-size: 20px; }
.folder-card { text-align: center; padding: 24px; position: relative; }
.folder-icon { font-size: 36px; margin-bottom: 8px; }
.folder-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.folder-category { margin-bottom: 8px; }
.folder-count { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.folder-default { font-size: 12px; color: var(--color-success); font-weight: 500; margin-bottom: 8px; }
.folder-actions { display: flex; gap: 8px; justify-content: center; }
</style>
