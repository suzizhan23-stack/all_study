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
      <button class="btn btn-sm btn-danger" @click="batchDelete" :disabled="!selectedIds.length">批量删除</button>
      <button class="btn btn-sm">批量加标签</button>
    </div>
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--color-text-secondary)">加载中...</div>
    <template v-else>
      <table class="table" style="margin-top:12px">
        <thead>
          <tr>
            <th style="width:30px"><input type="checkbox" @change="toggleSelectAll" :checked="allSelected" /></th>
            <th>单词</th>
            <th>释义</th>
            <th>频率</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td><input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelect(item.id)" /></td>
            <td><router-link :to="`/word/${item.entity_id || item.entityId}`" style="font-weight:600">{{ item.word }}</router-link></td>
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
      <div v-if="!items.length" style="text-align:center;padding:40px;color:var(--color-text-secondary)">暂无条目</div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { folderApi, favoriteApi } from '../api'

const route = useRoute()
const folderId = computed(() => route.params.id)
const loading = ref(false)

const folder = ref({ name: '收藏夹', category: '' })
const items = ref([])
const selectedIds = ref([])
const allSelected = computed(() => items.value.length > 0 && selectedIds.value.length === items.value.length)

async function fetchItems() {
  loading.value = true
  try {
    const res = await folderApi.getItems(folderId.value, { page: 1, size: 50, sort: 'created' })
    if (res && res.items) {
      items.value = res.items
      if (res.folder) folder.value = res.folder
    } else if (Array.isArray(res)) {
      items.value = res
    } else {
      items.value = []
    }
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function toggleSelect(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = items.value.map(i => i.id)
  }
}

async function removeItem(id) {
  try {
    await favoriteApi.remove(id)
    await fetchItems()
  } catch {}
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  try {
    await favoriteApi.batchDelete(selectedIds.value)
    selectedIds.value = []
    await fetchItems()
  } catch {}
}

onMounted(fetchItems)
</script>

<style scoped>
.folder-detail-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
.folder-detail-header h2 { flex: 1; font-size: 20px; }
.folder-meta { display: flex; gap: 12px; font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.folder-actions-bar { display: flex; gap: 8px; }
</style>
