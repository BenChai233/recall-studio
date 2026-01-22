<template>
  <section class="grid cols-3">
    <div class="stat fade-up" style="--delay: 0s">
      <div class="pill">到期</div>
      <div class="value">{{ today?.dueCount ?? 0 }}</div>
      <div class="muted">需要复习的题目</div>
    </div>
    <div class="stat fade-up" style="--delay: 0.08s">
      <div class="pill">错题复测</div>
      <div class="value">{{ today?.wrongCount ?? 0 }}</div>
      <div class="muted">优先回访</div>
    </div>
    <div class="stat fade-up" style="--delay: 0.16s">
      <div class="pill">新题</div>
      <div class="value">{{ today?.newCount ?? 0 }}</div>
      <div class="muted">可选补充</div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section class="grid cols-2">
    <div class="card">
      <h3 class="card-title">今日焦点</h3>
      <div v-if="loading" class="muted">正在载入任务清单…</div>
      <div v-else-if="focusItems.length === 0" class="empty">
        暂无需要处理的题目
      </div>
      <div v-else class="list">
        <div v-for="item in focusItems" :key="item.itemId" class="list-item">
          <div>
            <div>{{ item.prompt }}</div>
            <div class="muted">{{ item.deckName }}</div>
          </div>
          <span class="chip">{{ item.dueLabel }}</span>
        </div>
      </div>
    </div>
    <div class="card soft">
      <h3 class="card-title">快速入口</h3>
      <div class="list">
        <RouterLink
          v-for="action in actions"
          :key="action.label"
          class="list-item"
          :to="action.to"
        >
          <span>{{ action.label }}</span>
          <span class="chip">{{ action.note }}</span>
        </RouterLink>
      </div>
    </div>
  </section>

  <section class="grid cols-3">
    <div class="card">
      <h3 class="card-title">今日计划</h3>
      <div class="value">{{ today?.totalPlanned ?? 0 }} 题</div>
      <p class="muted">按“到期 → 错题 → 新题”编排。</p>
    </div>
    <div class="card">
      <h3 class="card-title">复习节奏</h3>
      <p class="muted">建议 20–30 分钟内完成一轮。</p>
      <div class="chip">自动均衡</div>
    </div>
    <div class="card">
      <h3 class="card-title">同步提醒</h3>
      <p class="muted">开始前先 pull，结束后 add/commit/push。</p>
      <div v-if="syncError" class="muted">{{ syncError }}</div>
      <div v-else-if="syncStatus" class="list">
        <div class="list-item">
          <span>上次同步</span>
          <span class="chip">{{ formatTime(syncStatus.lastSyncAt) }}</span>
        </div>
        <div class="list-item">
          <span>本地变更</span>
          <span class="chip">{{ syncStatus.dirty ? '有' : '无' }}</span>
        </div>
        <div class="list-item">
          <span>数据文件</span>
          <span class="chip">{{ syncStatus.fileCount }}</span>
        </div>
        <div class="list-item">
          <span>最新改动</span>
          <span class="chip">{{ formatTime(syncStatus.lastModifiedAt) }}</span>
        </div>
      </div>
      <div class="muted" style="margin-top: 8px">{{ syncHint }}</div>
      <button class="btn" type="button" :disabled="syncing" @click="markSynced">
        标记已同步
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getSyncStatus, getTodayStats, listDecks, listItems, markSyncSnapshot } from '../api'
import { pushToast } from '../composables/toast'
import type { Deck, Item, SyncStatus, TodayStats } from '../api/types'
import { stripMarkdown } from '../utils/markdown'

const today = ref<TodayStats | null>(null)
const decks = ref<Deck[]>([])
const items = ref<Item[]>([])
const loading = ref(false)
const error = ref('')
const syncStatus = ref<SyncStatus | null>(null)
const syncError = ref('')
const syncing = ref(false)

const actions = [
  { label: '开始复习会话', note: '进入复习', to: '/review' },
  { label: '查看题库', note: '管理牌组', to: '/decks' },
  { label: '调整参数', note: 'SRS 配置', to: '/settings' },
]

const deckMap = computed(() => {
  const map = new Map<string, string>()
  decks.value.forEach((deck) => map.set(deck.deckId, deck.name))
  return map
})

const focusItems = computed(() => {
  const now = new Date()
  return items.value
    .filter((item) => !item.archived)
    .map((item) => {
      const srs = item.srs
      const wrongDue = srs?.wrongDue ? new Date(srs.wrongDue) : null
      const due = srs?.due ? new Date(srs.due) : null
      let dueLabel = '计划'
      if (wrongDue && wrongDue <= now) {
        dueLabel = '错题复测'
      } else if (due && due <= now) {
        dueLabel = '到期'
      } else if (!srs) {
        dueLabel = '新题'
      }
      return {
        itemId: item.itemId,
        prompt: stripMarkdown(item.prompt, 60) || '未填写题干',
        deckName: deckMap.value.get(item.deckId) || '未命名牌组',
        dueLabel,
        dueDate: wrongDue ?? due ?? new Date(8640000000000000),
      }
    })
    .sort((a, b) => a.dueDate.getTime() - b.dueDate.getTime())
    .slice(0, 4)
})

const syncHint = computed(() => {
  if (!syncStatus.value) return '同步状态暂不可用。'
  if (!syncStatus.value.hasSnapshot) return '尚未记录同步快照，可在完成同步后点击标记。'
  if (syncStatus.value.dirty) return '检测到本地变更，建议完成 commit/push 后再标记。'
  return '本地数据已同步。'
})

const formatTime = (value?: string | null) => {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未记录'
  return date.toLocaleString()
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [todayData, deckData, itemData] = await Promise.all([
      getTodayStats(),
      listDecks(),
      listItems(),
    ])
    today.value = todayData
    decks.value = deckData
    items.value = itemData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }

  syncError.value = ''
  try {
    syncStatus.value = await getSyncStatus()
  } catch (err) {
    syncError.value = err instanceof Error ? err.message : '同步状态获取失败'
  }
}

const markSynced = async () => {
  syncing.value = true
  syncError.value = ''
  try {
    syncStatus.value = await markSyncSnapshot()
    pushToast('已记录同步快照', 'success')
  } catch (err) {
    syncError.value = err instanceof Error ? err.message : '同步标记失败'
    pushToast('同步标记失败', 'error')
  } finally {
    syncing.value = false
  }
}

onMounted(load)
</script>
