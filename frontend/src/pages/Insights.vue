<template>
  <section class="grid cols-3">
    <div class="card">
      <h3 class="card-title">题目总量</h3>
      <div class="value">{{ totalItems }}</div>
      <div class="muted">已录入的题目数量</div>
    </div>
    <div class="card">
      <h3 class="card-title">今日待复习</h3>
      <div class="value">{{ today?.totalPlanned ?? 0 }}</div>
      <div class="muted">到期 + 错题 + 新题</div>
    </div>
    <div class="card">
      <h3 class="card-title">到期率</h3>
      <div class="value">{{ dueRate }}%</div>
      <div class="muted">当前到期题占比</div>
    </div>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section class="card">
    <h3 class="card-title">今日任务构成</h3>
    <div class="bars">
      <div class="bar-row">
        <span>到期题</span>
        <div class="bar"><div class="bar-fill" :style="{ width: duePercent + '%' }"></div></div>
        <span class="muted">{{ today?.dueCount ?? 0 }}</span>
      </div>
      <div class="bar-row">
        <span>错题复测</span>
        <div class="bar"><div class="bar-fill" :style="{ width: wrongPercent + '%' }"></div></div>
        <span class="muted">{{ today?.wrongCount ?? 0 }}</span>
      </div>
      <div class="bar-row">
        <span>新题</span>
        <div class="bar"><div class="bar-fill" :style="{ width: newPercent + '%' }"></div></div>
        <span class="muted">{{ today?.newCount ?? 0 }}</span>
      </div>
    </div>
  </section>

  <section class="card soft">
    <h3 class="card-title">错因分布</h3>
    <div v-if="reasonError" class="alert">{{ reasonError }}</div>
    <div
      v-else-if="!reasonDistribution || reasonDistribution.stats.length === 0"
      class="muted"
    >
      暂无错题记录，完成复习后会自动统计。
    </div>
    <div v-else class="bars">
      <div v-for="stat in reasonDistribution.stats" :key="stat.tag" class="bar-row">
        <span>{{ stat.tag }}</span>
        <div class="bar">
          <div class="bar-fill" :style="{ width: reasonPercent(stat.count) + '%' }"></div>
        </div>
        <span class="muted">{{ stat.count }}</span>
      </div>
    </div>
    <div
      v-if="reasonDistribution && reasonDistribution.stats.length > 0"
      class="muted"
      style="margin-top: 12px"
    >
      共 {{ reasonDistribution.totalTagCount }} 次错因记录，覆盖
      {{ reasonDistribution.wrongReviewCount }} 次错题。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getReasonDistribution, getTodayStats, listItems } from '../api'
import type { Item, ReasonDistribution, TodayStats } from '../api/types'

const today = ref<TodayStats | null>(null)
const items = ref<Item[]>([])
const error = ref('')
const reasonDistribution = ref<ReasonDistribution | null>(null)
const reasonError = ref('')

const totalItems = computed(() => items.value.filter((item) => !item.archived).length)

const dueItems = computed(() => {
  const now = new Date()
  return items.value.filter((item) => {
    if (item.archived) return false
    const wrongDue = item.srs?.wrongDue ? new Date(item.srs.wrongDue) : null
    const due = item.srs?.due ? new Date(item.srs.due) : null
    return (wrongDue && wrongDue <= now) || (due && due <= now)
  }).length
})

const dueRate = computed(() => {
  if (totalItems.value === 0) return 0
  return Math.round((dueItems.value / totalItems.value) * 100)
})

const duePercent = computed(() => {
  if (!today.value || today.value.totalPlanned === 0) return 0
  return Math.round((today.value.dueCount / today.value.totalPlanned) * 100)
})

const wrongPercent = computed(() => {
  if (!today.value || today.value.totalPlanned === 0) return 0
  return Math.round((today.value.wrongCount / today.value.totalPlanned) * 100)
})

const newPercent = computed(() => {
  if (!today.value || today.value.totalPlanned === 0) return 0
  return Math.round((today.value.newCount / today.value.totalPlanned) * 100)
})

const reasonPercent = (count: number) => {
  const total = reasonDistribution.value?.totalTagCount ?? 0
  if (total === 0) return 0
  return Math.round((count / total) * 100)
}

const load = async () => {
  error.value = ''
  try {
    const [todayData, itemData] = await Promise.all([getTodayStats(), listItems()])
    today.value = todayData
    items.value = itemData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  }

  reasonError.value = ''
  try {
    reasonDistribution.value = await getReasonDistribution()
  } catch (err) {
    reasonError.value = err instanceof Error ? err.message : '错因统计加载失败'
  }
}

onMounted(load)
</script>

<style scoped>
.bars {
  display: grid;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 60px;
  gap: 12px;
  align-items: center;
}

.bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(15, 76, 92, 0.15);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, rgba(15, 76, 92, 0.7), rgba(232, 93, 63, 0.9));
}
</style>
