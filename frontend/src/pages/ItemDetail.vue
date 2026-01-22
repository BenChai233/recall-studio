<template>
  <section class="card">
    <div class="detail-header">
      <div>
        <div class="breadcrumb">
          <RouterLink class="link" to="/decks">题库</RouterLink>
          <span class="muted">/</span>
          <RouterLink v-if="deckId" class="link" :to="`/decks/${deckId}`">
            专题
          </RouterLink>
          <span class="muted">/</span>
          <span>题目详情</span>
        </div>
        <h2 class="section-title">题目详情</h2>
        <div class="muted">查看题干、要点与题目元信息。</div>
      </div>
      <div class="detail-actions">
        <button class="btn ghost" type="button" :disabled="undoing" @click="undoReview">
          撤回最近一次复习
        </button>
        <RouterLink v-if="deckId" class="btn ghost" :to="`/decks/${deckId}`">
          返回专题
        </RouterLink>
      </div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section v-if="loading" class="card soft">正在加载题目详情…</section>

  <section v-else-if="item" class="grid cols-2">
    <div class="card">
      <h3 class="card-title">题干（Markdown）</h3>
      <div class="markdown-body" v-html="promptHtml"></div>
      <div class="meta">
        <span class="chip">{{ item.type }}</span>
        <span class="chip">{{ item.difficulty || '未设置难度' }}</span>
        <span class="chip">{{ item.archived ? '已归档' : '使用中' }}</span>
      </div>
    </div>
    <div class="card soft">
      <h3 class="card-title">答案要点（Markdown）</h3>
      <div v-if="answerHtml" class="markdown-body" v-html="answerHtml"></div>
      <div v-else class="muted">暂无答案要点。</div>
      <div class="meta">
        <span class="tag" v-for="tag in item.tags || []" :key="tag">{{ tag }}</span>
        <span v-if="(item.tags || []).length === 0" class="muted">无标签</span>
      </div>
    </div>
  </section>

  <section v-if="item" class="card">
    <h3 class="card-title">我的作答</h3>
    <div v-if="latestReview?.answer" class="markdown-body" v-html="latestAnswerHtml"></div>
    <div v-else class="muted">暂无复习作答记录。</div>
    <div v-if="latestReview" class="meta">
      <span class="chip">评分：{{ latestReview.score }}</span>
      <span class="chip">时间：{{ formatTime(latestReview.reviewedAt) }}</span>
      <span v-if="latestReview.reasonTags?.length" class="tag">
        {{ latestReview.reasonTags.join(' / ') }}
      </span>
    </div>
  </section>

  <section v-if="item" class="card">
    <h3 class="card-title">补充信息</h3>
    <div class="info-grid">
      <div>
        <div class="muted">题目 ID</div>
        <div class="mono">{{ item.itemId }}</div>
      </div>
      <div>
        <div class="muted">所属专题</div>
        <div class="mono">{{ item.deckId }}</div>
      </div>
      <div>
        <div class="muted">创建时间</div>
        <div>{{ formatTime(item.createdAt) }}</div>
      </div>
      <div>
        <div class="muted">更新时间</div>
        <div>{{ formatTime(item.updatedAt) }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getItem, getLatestReviewAnswer, undoItemReview } from '../api'
import type { Item, ReviewAnswer } from '../api/types'
import { renderMarkdown } from '../utils/markdown'
import { pushToast } from '../composables/toast'

const route = useRoute()
const item = ref<Item | null>(null)
const latestReview = ref<ReviewAnswer | null>(null)
const loading = ref(false)
const undoing = ref(false)
const error = ref('')

const deckId = computed(() => (route.params.deckId as string) || '')
const itemId = computed(() => route.params.itemId as string)

const promptHtml = computed(() => (item.value ? renderMarkdown(item.value.prompt) : ''))
const answerHtml = computed(() => {
  if (!item.value?.answerMarkdown) return ''
  return renderMarkdown(item.value.answerMarkdown)
})
const latestAnswerHtml = computed(() => {
  if (!latestReview.value?.answer) return ''
  return renderMarkdown(latestReview.value.answer)
})

const formatTime = (value?: string) => {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未记录'
  return date.toLocaleString()
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [itemData, reviewData] = await Promise.all([
      getItem(itemId.value),
      getLatestReviewAnswer(itemId.value),
    ])
    item.value = itemData
    latestReview.value = reviewData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const undoReview = async () => {
  if (!item.value) return
  if (!window.confirm('确认撤回该题最近一次复习记录？')) return
  undoing.value = true
  error.value = ''
  try {
    await undoItemReview(item.value.itemId)
    pushToast('已撤回最近一次复习', 'success')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '撤回失败'
  } finally {
    undoing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.breadcrumb {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.link {
  color: inherit;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.mono {
  font-family: 'SFMono-Regular', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
    'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
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
