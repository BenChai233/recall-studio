<template>
  <section class="card">
    <div class="deck-header">
      <div>
        <h2 class="section-title">题库管理</h2>
        <div class="muted">集中管理专题与题目结构。</div>
      </div>
      <div class="deck-actions">
        <input ref="importInput" type="file" accept="application/json" hidden @change="handleImport" />
        <button class="btn ghost" type="button" @click="triggerImport">导入</button>
        <button class="btn ghost" type="button" @click="handleExport">导出</button>
        <button class="btn primary" type="button" @click="openCreate">新建专题</button>
      </div>
    </div>
    <div class="deck-filter">
      <input v-model="keyword" placeholder="按名称、标签筛选" />
      <button
        class="chip"
        :class="{ active: archiveFilter === 'active' }"
        type="button"
        @click="archiveFilter = 'active'"
      >
        仅显示活跃
      </button>
      <button
        class="chip"
        :class="{ active: archiveFilter === 'archived' }"
        type="button"
        @click="archiveFilter = 'archived'"
      >
        仅显示归档
      </button>
      <button
        class="chip"
        :class="{ active: archiveFilter === 'all' }"
        type="button"
        @click="archiveFilter = 'all'"
      >
        全部
      </button>
    </div>
  </section>

  <section v-if="showForm" class="card soft">
    <h3 class="card-title">{{ formTitle }}</h3>
    <div class="deck-form">
      <div class="field">
        <label>名称</label>
        <input v-model="formName" placeholder="如：Java 基础" />
      </div>
      <div class="field">
        <label>描述</label>
        <input v-model="formDescription" placeholder="如：JVM / 集合 / 并发" />
      </div>
      <div class="field">
        <label>标签（逗号分隔）</label>
        <input v-model="formTags" placeholder="java, backend" />
      </div>
      <div class="form-actions">
        <button class="btn ghost" type="button" @click="closeForm">取消</button>
        <button class="btn primary" type="button" @click="submitForm" :disabled="saving">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section v-if="loading" class="card soft">正在载入题库数据…</section>

  <section v-else-if="filteredDecks.length === 0" class="empty">
    还没有可展示的专题
  </section>

  <section v-else class="grid cols-3">
    <div
      v-for="(deck, index) in filteredDecks"
      :key="deck.deckId"
      class="card fade-up"
      :style="{ '--delay': `${index * 0.06}s` }"
    >
      <div class="deck-card">
        <div>
          <h3 class="card-title">{{ deck.name }}</h3>
          <p class="muted">{{ deck.description || '暂无描述' }}</p>
        </div>
        <div class="deck-meta">
          <span class="chip">{{ getStats(deck.deckId).total }} 题</span>
          <span class="tag">{{ getStats(deck.deckId).due }} 待复习</span>
        </div>
        <div class="deck-tags">
          <span v-for="tag in deck.tags || []" :key="tag" class="chip">{{ tag }}</span>
        </div>
        <div class="deck-ops">
          <RouterLink class="btn" :to="`/decks/${deck.deckId}`">进入专题</RouterLink>
          <button class="btn ghost" type="button" @click="openEdit(deck)">编辑</button>
          <button class="btn ghost" type="button" @click="toggleArchive(deck)">
            {{ deck.archived ? '恢复' : '归档' }}
          </button>
          <button class="btn ghost" type="button" @click="removeDeck(deck)">删除</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { createDeck, deleteDeck, exportData, importData, listDecks, listItems, updateDeck } from '../api'
import { pushToast } from '../composables/toast'
import type { Deck, Item } from '../api/types'

const decks = ref<Deck[]>([])
const items = ref<Item[]>([])
const keyword = ref('')
const archiveFilter = ref<'active' | 'archived' | 'all'>('active')
const loading = ref(false)
const error = ref('')
const saving = ref(false)

const showForm = ref(false)
const editingId = ref<string | null>(null)
const formName = ref('')
const formDescription = ref('')
const formTags = ref('')
const importInput = ref<HTMLInputElement | null>(null)

const deckStats = computed(() => {
  const now = new Date()
  const map = new Map<string, { total: number; due: number }>()
  items.value.forEach((item) => {
    if (!map.has(item.deckId)) {
      map.set(item.deckId, { total: 0, due: 0 })
    }
    const stat = map.get(item.deckId)!
    stat.total += 1
    const wrongDue = item.srs?.wrongDue ? new Date(item.srs.wrongDue) : null
    const due = item.srs?.due ? new Date(item.srs.due) : null
    if ((wrongDue && wrongDue <= now) || (due && due <= now)) {
      stat.due += 1
    }
  })
  return map
})

const filteredDecks = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return decks.value.filter((deck) => {
    if (archiveFilter.value === 'active' && deck.archived) return false
    if (archiveFilter.value === 'archived' && !deck.archived) return false
    if (!kw) return true
    const haystack = `${deck.name || ''}${deck.description || ''}${(deck.tags || []).join('')}`
    return haystack.toLowerCase().includes(kw)
  })
})

const getStats = (deckId: string) => {
  return deckStats.value.get(deckId) || { total: 0, due: 0 }
}

const openCreate = () => {
  editingId.value = null
  formName.value = ''
  formDescription.value = ''
  formTags.value = ''
  showForm.value = true
}

const openEdit = (deck: Deck) => {
  editingId.value = deck.deckId
  formName.value = deck.name
  formDescription.value = deck.description || ''
  formTags.value = (deck.tags || []).join(', ')
  showForm.value = true
}

const closeForm = () => {
  showForm.value = false
}

const submitForm = async () => {
  if (!formName.value.trim()) {
    pushToast('请填写专题名称', 'error')
    return
  }
  saving.value = true
  error.value = ''
  const tags = formTags.value
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0)
  try {
    if (editingId.value) {
      await updateDeck(editingId.value, {
        name: formName.value.trim(),
        description: formDescription.value.trim() || undefined,
        tags,
      })
      pushToast('专题已更新', 'success')
    } else {
      await createDeck({
        name: formName.value.trim(),
        description: formDescription.value.trim() || undefined,
        tags,
      })
      pushToast('专题已创建', 'success')
    }
    showForm.value = false
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

const toggleArchive = async (deck: Deck) => {
  try {
    await updateDeck(deck.deckId, { archived: !deck.archived })
    pushToast(deck.archived ? '已恢复专题' : '已归档专题', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  }
}

const removeDeck = async (deck: Deck) => {
  if (!window.confirm(`确认删除专题「${deck.name}」？`)) return
  try {
    await deleteDeck(deck.deckId)
    pushToast('已删除专题', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
}

const triggerImport = () => {
  importInput.value?.click()
}

const handleImport = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const file = input.files[0]
  try {
    const text = await file.text()
    const json = JSON.parse(text)
    const result = await importData({
      decks: json.decks || [],
      items: json.items || [],
    })
    pushToast(`导入完成：专题 ${result.importedDecks}，题目 ${result.importedItems}`, 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导入失败'
  } finally {
    input.value = ''
  }
}

const handleExport = async () => {
  try {
    const data = await exportData(false)
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `recall-export-${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    pushToast('导出成功', 'success')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导出失败'
  }
}

const formTitle = computed(() => (editingId.value ? '编辑专题' : '新建专题'))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [deckData, itemData] = await Promise.all([listDecks(), listItems()])
    decks.value = deckData
    items.value = itemData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.deck-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.deck-actions {
  display: flex;
  gap: 10px;
}

.deck-filter {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.deck-filter input {
  flex: 1;
  min-width: 220px;
}

.deck-card {
  display: grid;
  gap: 14px;
}

.deck-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.deck-form {
  display: grid;
  gap: 12px;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.deck-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.deck-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip.active {
  background: rgba(15, 76, 92, 0.15);
  border-color: rgba(15, 76, 92, 0.4);
}
</style>
