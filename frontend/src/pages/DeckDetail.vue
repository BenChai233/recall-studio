<template>
  <section class="card">
    <div class="deck-header">
      <div>
        <h2 class="section-title">{{ deck?.name || '牌组详情' }}</h2>
        <div class="muted">{{ deck?.description || '查看该牌组的题目清单' }}</div>
      </div>
      <div class="deck-actions">
        <button class="btn ghost" type="button" @click="handleExport">导出</button>
        <button class="btn primary" type="button" @click="openCreate">新增题目</button>
      </div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section v-if="showForm" class="card soft">
    <h3 class="card-title">{{ formTitle }}</h3>
    <div class="item-form">
      <div class="field">
        <label>题干</label>
        <textarea v-model="formPrompt" placeholder="输入题干"></textarea>
      </div>
      <div class="field">
        <label>题型</label>
        <select v-model="formType">
          <option value="concept">概念</option>
          <option value="mechanism">机制</option>
          <option value="scenario">场景</option>
          <option value="code">代码</option>
          <option value="choice">选择</option>
        </select>
      </div>
      <div class="field">
        <label>提示</label>
        <input v-model="formHint" placeholder="帮助回忆但不等同答案" />
      </div>
      <div class="field">
        <label>答案要点（逗号分隔）</label>
        <input v-model="formAnswerKey" placeholder="要点1, 要点2" />
      </div>
      <div class="field">
        <label>标签（逗号分隔）</label>
        <input v-model="formTags" placeholder="jvm, 多线程" />
      </div>
      <div class="field">
        <label>难度</label>
        <select v-model="formDifficulty">
          <option value="">未设置</option>
          <option value="easy">简单</option>
          <option value="medium">中等</option>
          <option value="hard">困难</option>
        </select>
      </div>
      <div class="form-actions">
        <button class="btn ghost" type="button" @click="closeForm">取消</button>
        <button class="btn primary" type="button" :disabled="saving" @click="submitForm">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>
  </section>

  <section class="card soft">
    <div class="deck-filter">
      <input v-model="keyword" placeholder="搜索题干" />
      <div class="chip">类型：不限</div>
      <div class="chip">难度：不限</div>
      <div class="chip">到期：7 天内</div>
    </div>
  </section>

  <section class="card">
    <h3 class="card-title">题目列表</h3>
    <div v-if="loading" class="muted">正在加载题目…</div>
    <div v-else-if="filteredItems.length === 0" class="empty">暂无题目</div>
    <div v-else class="list">
      <div v-for="item in filteredItems" :key="item.itemId" class="list-item">
        <div>
          <div>{{ item.prompt }}</div>
          <div class="muted">{{ item.type }} · {{ (item.tags || []).join(' / ') || '无标签' }}</div>
        </div>
        <div class="item-ops">
          <span class="chip">{{ formatStatus(item) }}</span>
          <button class="btn ghost" type="button" @click="openEdit(item)">编辑</button>
          <button class="btn ghost" type="button" @click="toggleArchive(item)">
            {{ item.archived ? '恢复' : '归档' }}
          </button>
          <button class="btn ghost" type="button" @click="removeItem(item)">删除</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { createItem, deleteItem, exportData, getDeck, listItems, updateItem } from '../api'
import { pushToast } from '../composables/toast'
import type { Deck, Item } from '../api/types'

const route = useRoute()
const deck = ref<Deck | null>(null)
const items = ref<Item[]>([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const saving = ref(false)

const showForm = ref(false)
const editingId = ref<string | null>(null)
const formPrompt = ref('')
const formType = ref('concept')
const formHint = ref('')
const formAnswerKey = ref('')
const formTags = ref('')
const formDifficulty = ref('')

const deckId = computed(() => route.params.deckId as string)

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter((item) => {
    if (!kw) return true
    const haystack = `${item.prompt}${item.type}${(item.tags || []).join('')}`
    return haystack.toLowerCase().includes(kw)
  })
})

const formatStatus = (item: Item) => {
  const now = new Date()
  const wrongDue = item.srs?.wrongDue ? new Date(item.srs.wrongDue) : null
  const due = item.srs?.due ? new Date(item.srs.due) : null
  if (wrongDue && wrongDue <= now) return '错题复测'
  if (due && due <= now) return '到期'
  if (!item.srs) return '新题'
  return '计划中'
}

const openCreate = () => {
  editingId.value = null
  formPrompt.value = ''
  formType.value = 'concept'
  formHint.value = ''
  formAnswerKey.value = ''
  formTags.value = ''
  formDifficulty.value = ''
  showForm.value = true
}

const openEdit = (item: Item) => {
  editingId.value = item.itemId
  formPrompt.value = item.prompt
  formType.value = item.type
  formHint.value = item.hint || ''
  formAnswerKey.value = (item.answerKey || []).join(', ')
  formTags.value = (item.tags || []).join(', ')
  formDifficulty.value = item.difficulty || ''
  showForm.value = true
}

const closeForm = () => {
  showForm.value = false
}

const submitForm = async () => {
  if (!formPrompt.value.trim()) {
    pushToast('题干不能为空', 'error')
    return
  }
  saving.value = true
  error.value = ''
  const answerKey = formAnswerKey.value
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0)
  const tags = formTags.value
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0)
  try {
    if (editingId.value) {
      await updateItem(editingId.value, {
        deckId: deckId.value,
        type: formType.value,
        prompt: formPrompt.value.trim(),
        hint: formHint.value.trim() || undefined,
        answerKey,
        tags,
        difficulty: formDifficulty.value || undefined,
      })
      pushToast('题目已更新', 'success')
    } else {
      await createItem({
        deckId: deckId.value,
        type: formType.value,
        prompt: formPrompt.value.trim(),
        hint: formHint.value.trim() || undefined,
        answerKey,
        tags,
        difficulty: formDifficulty.value || undefined,
      })
      pushToast('题目已创建', 'success')
    }
    showForm.value = false
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

const toggleArchive = async (item: Item) => {
  try {
    await updateItem(item.itemId, { archived: !item.archived })
    pushToast(item.archived ? '题目已恢复' : '题目已归档', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  }
}

const removeItem = async (item: Item) => {
  if (!window.confirm('确认删除该题目？')) return
  try {
    await deleteItem(item.itemId)
    pushToast('题目已删除', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
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
    link.download = `deck-${deckId.value}-${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    pushToast('导出成功', 'success')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导出失败'
  }
}

const formTitle = computed(() => (editingId.value ? '编辑题目' : '新增题目'))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    deck.value = await getDeck(deckId.value)
    items.value = await listItems({ deckId: deckId.value })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(deckId, load)
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
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.deck-filter input {
  flex: 1;
  min-width: 220px;
}

.item-form {
  display: grid;
  gap: 12px;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.item-ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
