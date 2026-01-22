<template>
  <section class="card">
    <h2 class="section-title">设置</h2>
    <p class="muted">调整 SRS 参数与会话偏好。</p>
  </section>

  <section v-if="error" class="alert">{{ error }}</section>

  <section class="grid cols-2">
    <div class="card">
      <h3 class="card-title">数据仓库</h3>
      <div class="field">
        <label>数据目录</label>
        <input value="../database" disabled />
      </div>
      <div class="field">
        <label>同步模式</label>
        <div class="chip">手动 Git</div>
      </div>
      <div class="muted">数据目录需在后端配置中修改。</div>
    </div>

    <div class="card soft">
      <h3 class="card-title">SRS 参数</h3>
      <div v-if="loading" class="muted">正在加载设置…</div>
      <div v-else class="settings-form">
        <div class="field">
          <label>每日上限</label>
          <input v-model.number="dailyLimit" type="number" min="1" />
        </div>
        <div class="field">
          <label>新题比例</label>
          <input v-model.number="newRatio" type="number" step="0.1" min="0" max="1" />
        </div>
        <div class="field">
          <label>错题复测间隔</label>
          <input v-model="wrongSchedule" placeholder="如 1,3,7" />
        </div>
        <div class="field">
          <label>算法</label>
          <input :value="settings?.srsAlgo || 'SM2_SIMPLE'" disabled />
        </div>
        <button class="btn primary" type="button" @click="save" :disabled="saving">
          保存设置
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getSettings, updateSettings } from '../api'
import { pushToast } from '../composables/toast'
import type { Settings } from '../api/types'

const settings = ref<Settings | null>(null)
const dailyLimit = ref(50)
const newRatio = ref(0.2)
const wrongSchedule = ref('1,3,7')
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    settings.value = await getSettings()
    dailyLimit.value = settings.value.dailyLimit
    newRatio.value = settings.value.newRatio
    wrongSchedule.value = settings.value.wrongReviewSchedule.join(',')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!settings.value) return
  saving.value = true
  error.value = ''
  try {
    const updated: Settings = {
      ...settings.value,
      dailyLimit: Number(dailyLimit.value),
      newRatio: Number(newRatio.value),
      wrongReviewSchedule: wrongSchedule.value
        .split(',')
        .map((v) => Number(v.trim()))
        .filter((v) => !Number.isNaN(v)),
    }
    settings.value = await updateSettings(updated)
    pushToast('设置已保存', 'success')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
