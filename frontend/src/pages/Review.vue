<template>
  <section class="card">
    <div class="review-header">
      <div>
        <h2 class="section-title">复习会话</h2>
        <div class="muted">闭卷作答 + 自评评分，自动更新调度。</div>
      </div>
      <div class="review-actions">
        <select v-model="selectedDeckId">
          <option value="">全部牌组</option>
          <option v-for="deck in decks" :key="deck.deckId" :value="deck.deckId">
            {{ deck.name }}
          </option>
        </select>
        <label class="chip">
          <input type="checkbox" v-model="onlyWrong" />
          仅复测错题
        </label>
        <button class="btn primary" type="button" @click="startSession">开始会话</button>
      </div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section v-if="!session && !loadingSession" class="card soft">
    选择牌组并点击“开始会话”以生成今日复习列表。
  </section>

  <section v-if="session && currentItem" class="grid cols-2">
    <div class="card">
      <h3 class="card-title">题干</h3>
      <div class="prompt markdown-body" v-html="promptHtml"></div>
      <div class="prompt-meta">
        <span class="chip">{{ currentItem.type }}</span>
        <span class="tag">{{ dueLabel(currentItem.dueType) }}</span>
        <span class="chip">第 {{ currentIndex + 1 }} / {{ session.items.length }} 题</span>
      </div>
    </div>
    <div class="card soft">
      <h3 class="card-title">作答</h3>
      <textarea v-model="answer" class="answer" placeholder="输入你的答案..."></textarea>
      <div class="reason">
        <span class="muted">错因标记：</span>
        <label v-for="tag in reasonOptions" :key="tag" class="chip">
          <input type="checkbox" :value="tag" v-model="reasonTags" />
          {{ tag }}
        </label>
      </div>
    </div>
  </section>

  <section v-if="session && currentItem" class="card">
    <div class="rating-row">
      <div>
        <h3 class="card-title">自评分数</h3>
        <div class="muted">选择 0/1/2 更新调度。</div>
      </div>
      <div class="rating-buttons">
        <button
          type="button"
          :disabled="submitting"
          :class="['btn', pendingScore === 0 ? 'primary' : 'ghost', { selected: pendingScore === 0 }]"
          @click="selectScore(0)"
        >
          0 再来
        </button>
        <button
          type="button"
          :disabled="submitting"
          :class="['btn', pendingScore === 1 ? 'primary' : 'ghost', { selected: pendingScore === 1 }]"
          @click="selectScore(1)"
        >
          1 吃力
        </button>
        <button
          type="button"
          :disabled="submitting"
          :class="['btn', pendingScore === 2 ? 'primary' : 'ghost', { selected: pendingScore === 2 }]"
          @click="selectScore(2)"
        >
          2 掌握
        </button>
        <button class="btn ghost" type="button" :disabled="!canUndo" @click="undoLast">
          撤回上一题
        </button>
      </div>
    </div>
    <div v-if="pendingScore !== null" class="confirm-row">
      <div class="muted">已选择评分：{{ scoreLabel(pendingScore) }}</div>
      <div class="confirm-actions">
        <button class="btn ghost" type="button" :disabled="submitting" @click="pendingScore = null">
          返回修改
        </button>
        <button class="btn primary" type="button" :disabled="submitting" @click="confirmSubmit">
          确认提交
        </button>
      </div>
    </div>
  </section>

  <section v-if="session && !currentItem && summary" class="card">
    <h3 class="card-title">会话复盘</h3>
    <div class="grid cols-3">
      <div class="stat">
        <div class="pill">平均分</div>
        <div class="value">{{ summary.avgScore.toFixed(2) }}</div>
      </div>
      <div class="stat">
        <div class="pill">0 分</div>
        <div class="value">{{ summary.scoreCount['0'] ?? 0 }}</div>
      </div>
      <div class="stat">
        <div class="pill">2 分</div>
        <div class="value">{{ summary.scoreCount['2'] ?? 0 }}</div>
      </div>
    </div>
    <div class="list" style="margin-top: 16px">
      <div v-for="item in summary.wrongItems" :key="item.itemId" class="list-item">
        <div>{{ promptSummary(item.prompt || '') }}</div>
        <span class="chip">错题</span>
      </div>
    </div>
    <div class="card soft" style="margin-top: 16px">
      <h4 class="card-title">下次优先自问的问题（可编辑）</h4>
      <div v-if="editableQuestions.length === 0" class="empty">暂无问题，可自行补充</div>
      <div v-else class="list">
        <div v-for="(question, index) in editableQuestions" :key="index" class="list-item">
          <input v-model="editableQuestions[index]" />
        </div>
      </div>
      <div class="form-actions" style="margin-top: 12px">
        <button class="btn ghost" type="button" @click="editableQuestions.push('')">新增一条</button>
      </div>
    </div>
    <div class="form-actions" style="margin-top: 12px">
      <button class="btn ghost" type="button" :disabled="!canUndo" @click="undoLast">
        撤回上一题
      </button>
      <button class="btn" type="button" @click="startSession">再来一轮</button>
    </div>
  </section>

  <section v-if="loadingSession" class="card soft">正在生成会话…</section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createSession, getSessionSummary, listDecks, submitReview, undoLastReview } from '../api'
import { pushToast } from '../composables/toast'
import type { Deck, SessionResponse, SessionSummary } from '../api/types'
import { renderMarkdown, stripMarkdown } from '../utils/markdown'

const decks = ref<Deck[]>([])
const selectedDeckId = ref('')
const onlyWrong = ref(false)

const session = ref<SessionResponse | null>(null)
const summary = ref<SessionSummary | null>(null)
const answer = ref('')
const reasonTags = ref<string[]>([])
const currentIndex = ref(0)
const error = ref('')
const loadingSession = ref(false)
const submitting = ref(false)
const editableQuestions = ref<string[]>([])
const pendingScore = ref<number | null>(null)

const reasonOptions = ['概念', '机制', '边界', '工程实践']

const currentItem = computed(() => {
  if (!session.value) return null
  return session.value.items[currentIndex.value] || null
})

const canUndo = computed(() => {
  return (
    !!session.value &&
    currentIndex.value > 0 &&
    !submitting.value &&
    pendingScore.value === null
  )
})

const promptHtml = computed(() => {
  if (!currentItem.value) return ''
  return renderMarkdown(currentItem.value.prompt)
})

const promptSummary = (prompt: string) => stripMarkdown(prompt, 80) || '未记录题干'

const dueLabel = (dueType: string) => {
  if (dueType === 'WRONG') return '错题复测'
  if (dueType === 'NEW') return '新题'
  return '到期'
}

const scoreLabel = (score: number) => {
  if (score === 0) return '0 再来'
  if (score === 1) return '1 吃力'
  return '2 掌握'
}

const startSession = async () => {
  loadingSession.value = true
  error.value = ''
  summary.value = null
  currentIndex.value = 0
  answer.value = ''
  reasonTags.value = []
  editableQuestions.value = []
  pendingScore.value = null
  try {
    session.value = await createSession({
      deckId: selectedDeckId.value || null,
      onlyWrong: onlyWrong.value,
      limit: 20,
    })
    if (session.value.items.length === 0) {
      error.value = '当前没有可复习的题目'
      pushToast('当前没有可复习的题目', 'info')
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建会话失败'
  } finally {
    loadingSession.value = false
  }
}

const submitScore = async (score: number) => {
  if (!session.value || !currentItem.value) return
  submitting.value = true
  error.value = ''
  try {
    await submitReview({
      sessionId: session.value.sessionId,
      itemId: currentItem.value.itemId,
      score,
      answer: answer.value,
      reasonTags: reasonTags.value,
    })
    answer.value = ''
    reasonTags.value = []
    if (currentIndex.value + 1 < session.value.items.length) {
      currentIndex.value += 1
    } else {
      currentIndex.value = session.value.items.length
      summary.value = await getSessionSummary(session.value.sessionId)
      editableQuestions.value = summary.value.nextQuestions.length
        ? [...summary.value.nextQuestions]
        : []
      pushToast('会话已完成', 'success')
    }
    pendingScore.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

const selectScore = (score: number) => {
  if (submitting.value) return
  pendingScore.value = score
}

const undoLast = async () => {
  if (!session.value || currentIndex.value <= 0) return
  submitting.value = true
  error.value = ''
  try {
    const result = await undoLastReview(session.value.sessionId)
    summary.value = null
    const targetIndex = session.value.items.findIndex((item) => item.itemId === result.itemId)
    currentIndex.value = targetIndex >= 0 ? targetIndex : Math.max(0, currentIndex.value - 1)
    answer.value = result.answer || ''
    reasonTags.value = result.reasonTags ?? []
    pendingScore.value = result.score
    pushToast('已撤回上一题', 'info')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '撤回失败'
  } finally {
    submitting.value = false
  }
}

const confirmSubmit = async () => {
  if (pendingScore.value === null) return
  await submitScore(pendingScore.value)
}

const loadDecks = async () => {
  try {
    decks.value = await listDecks()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载牌组失败'
  }
}

onMounted(loadDecks)
</script>

<style scoped>
.review-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.review-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

select {
  border-radius: 999px;
  border: 1px solid var(--line);
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.8);
}

.prompt {
  font-size: 18px;
  line-height: 1.5;
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

.prompt-meta {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.answer {
  min-height: 180px;
}

.reason {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  align-items: center;
}

.rating-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.rating-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.rating-buttons .btn.selected {
  box-shadow: 0 0 0 2px rgba(15, 76, 92, 0.3);
  border-color: rgba(15, 76, 92, 0.5);
}

.rating-buttons .btn.selected:not(.primary) {
  background: rgba(15, 76, 92, 0.12);
  color: var(--accent-2);
}

.confirm-row {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(15, 76, 92, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
