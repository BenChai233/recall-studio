<template>
  <section class="card">
    <div class="deck-header">
      <div>
        <h2 class="section-title">{{ deck?.name || '专题详情' }}</h2>
        <div class="muted">{{ deck?.description || '查看该专题的题目清单' }}</div>
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
        <label>题干（Markdown）</label>
        <div class="markdown-editor">
          <div class="editor-toolbar">
            <button class="btn ghost" type="button" @click="wrapSelection('prompt', '**', '**')">
              加粗
            </button>
            <button class="btn ghost" type="button" @click="wrapSelection('prompt', '*', '*')">
              斜体
            </button>
            <button class="btn ghost" type="button" @click="wrapSelection('prompt', '`', '`')">
              代码
            </button>
            <button
              class="btn ghost"
              type="button"
              @click="insertBlock('prompt', '```\\n', '\\n```')"
            >
              代码块
            </button>
            <button class="btn ghost" type="button" @click="insertLine('prompt', '- ')">
              无序列表
            </button>
            <button class="btn ghost" type="button" @click="insertLine('prompt', '1. ')">
              有序列表
            </button>
          </div>
          <textarea
            ref="promptTextarea"
            v-model="formPrompt"
            placeholder="使用 Markdown 编写题干，支持多行。"
          ></textarea>
        </div>
        <div class="preview card soft">
          <div class="muted">预览</div>
          <div class="markdown-body" v-html="promptPreview"></div>
        </div>
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
        <label>答案要点（Markdown）</label>
        <div class="markdown-editor">
          <div class="editor-toolbar">
            <button
              class="btn ghost"
              type="button"
              @click="wrapSelection('answer', '**', '**')"
            >
              加粗
            </button>
            <button class="btn ghost" type="button" @click="wrapSelection('answer', '*', '*')">
              斜体
            </button>
            <button class="btn ghost" type="button" @click="wrapSelection('answer', '`', '`')">
              代码
            </button>
            <button
              class="btn ghost"
              type="button"
              @click="insertBlock('answer', '```\\n', '\\n```')"
            >
              代码块
            </button>
            <button class="btn ghost" type="button" @click="insertLine('answer', '- ')">
              无序列表
            </button>
            <button class="btn ghost" type="button" @click="insertLine('answer', '1. ')">
              有序列表
            </button>
          </div>
          <textarea
            ref="answerTextarea"
            v-model="formAnswerMarkdown"
            placeholder="使用 Markdown 编写答案要点，支持多行。"
          ></textarea>
        </div>
        <div class="preview card soft">
          <div class="muted">预览</div>
          <div class="markdown-body" v-html="answerPreview"></div>
        </div>
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
        <RouterLink class="item-main" :to="`/decks/${deckId}/items/${item.itemId}`">
          <div class="item-title">{{ promptSummary(item.prompt) }}</div>
          <div class="muted">{{ item.type }} · {{ (item.tags || []).join(' / ') || '无标签' }}</div>
        </RouterLink>
        <div class="item-ops">
          <span class="chip">{{ formatStatus(item) }}</span>
          <button
            class="btn ghost"
            type="button"
            :disabled="undoingId === item.itemId"
            @click="undoItemReviewAction(item)"
          >
            撤回复习
          </button>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  createItem,
  deleteItem,
  exportData,
  getDeck,
  listItems,
  undoItemReview,
  updateItem,
} from '../api'
import { RouterLink } from 'vue-router'
import { pushToast } from '../composables/toast'
import type { Deck, Item } from '../api/types'
import { renderMarkdown, stripMarkdown } from '../utils/markdown'

const route = useRoute()
const deck = ref<Deck | null>(null)
const items = ref<Item[]>([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const undoingId = ref<string | null>(null)

const showForm = ref(false)
const editingId = ref<string | null>(null)
const formPrompt = ref('')
const formType = ref('concept')
const formHint = ref('')
const formAnswerMarkdown = ref('')
const formTags = ref('')
const formDifficulty = ref('')
const promptTextarea = ref<HTMLTextAreaElement | null>(null)
const answerTextarea = ref<HTMLTextAreaElement | null>(null)

const deckId = computed(() => route.params.deckId as string)

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter((item) => {
    if (!kw) return true
    const promptText = stripMarkdown(item.prompt, 200)
    const haystack = `${promptText}${item.type}${(item.tags || []).join('')}`
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
  formAnswerMarkdown.value = ''
  formTags.value = ''
  formDifficulty.value = ''
  showForm.value = true
}

const openEdit = (item: Item) => {
  editingId.value = item.itemId
  formPrompt.value = item.prompt
  formType.value = item.type
  formHint.value = item.hint || ''
  formAnswerMarkdown.value = item.answerMarkdown || ''
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
  const answerMarkdown = normalizeAnswerMarkdown()
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
        answerMarkdown,
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
        answerMarkdown,
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

const undoItemReviewAction = async (item: Item) => {
  if (!window.confirm('确认撤回该题最近一次复习记录？')) return
  undoingId.value = item.itemId
  error.value = ''
  try {
    await undoItemReview(item.itemId)
    pushToast('已撤回最近一次复习', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '撤回失败'
  } finally {
    undoingId.value = null
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

const normalizeAnswerMarkdown = () => {
  const plain = formAnswerMarkdown.value.trim()
  if (!plain) return undefined
  return plain
}

const getEditorTarget = (target: 'prompt' | 'answer') => {
  if (target === 'prompt') {
    return { el: promptTextarea.value, value: formPrompt }
  }
  return { el: answerTextarea.value, value: formAnswerMarkdown }
}

const wrapSelection = (target: 'prompt' | 'answer', before: string, after: string) => {
  const { el, value } = getEditorTarget(target)
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const current = value.value
  const selected = current.slice(start, end)
  value.value = current.slice(0, start) + before + selected + after + current.slice(end)
  nextTick(() => {
    el.focus()
    const cursor = start + before.length + selected.length + after.length
    el.selectionStart = cursor
    el.selectionEnd = cursor
  })
}

const insertBlock = (target: 'prompt' | 'answer', before: string, after: string) => {
  const { el, value } = getEditorTarget(target)
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const current = value.value
  const selected = current.slice(start, end)
  const block = `${before}${selected}${after}`
  value.value = current.slice(0, start) + block + current.slice(end)
  nextTick(() => {
    el.focus()
    const cursor = start + block.length
    el.selectionStart = cursor
    el.selectionEnd = cursor
  })
}

const insertLine = (target: 'prompt' | 'answer', prefix: string) => {
  const { el, value } = getEditorTarget(target)
  if (!el) return
  const start = el.selectionStart
  const current = value.value
  const lineStart = current.lastIndexOf('\n', start - 1) + 1
  value.value = current.slice(0, lineStart) + prefix + current.slice(lineStart)
  nextTick(() => {
    el.focus()
    const cursor = start + prefix.length
    el.selectionStart = cursor
    el.selectionEnd = cursor
  })
}

const promptSummary = (prompt: string) => {
  const summary = stripMarkdown(prompt, 80)
  return summary || '未填写题干'
}

const promptPreview = computed(() => {
  if (!formPrompt.value.trim()) return '<p class="muted">暂无内容</p>'
  return renderMarkdown(formPrompt.value)
})

const answerPreview = computed(() => {
  if (!formAnswerMarkdown.value.trim()) return '<p class="muted">暂无内容</p>'
  return renderMarkdown(formAnswerMarkdown.value)
})
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

.item-main {
  color: inherit;
  flex: 1;
  display: block;
  min-width: 0;
}

.item-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.markdown-editor {
  display: grid;
  gap: 8px;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.markdown-editor textarea {
  min-height: 160px;
}

.preview {
  margin-top: 10px;
}

.markdown-body {
  line-height: 1.7;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 18px;
  margin: 8px 0;
}

.markdown-body code {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(15, 76, 92, 0.12);
  font-family: 'SFMono-Regular', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
    'Liberation Mono', 'Courier New', monospace;
}

.markdown-body pre {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(15, 76, 92, 0.08);
  font-family: 'SFMono-Regular', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
    'Liberation Mono', 'Courier New', monospace;
  white-space: pre-wrap;
}
</style>
