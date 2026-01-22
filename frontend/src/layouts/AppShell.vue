<template>
  <div class="app-shell">
    <NavRail />
    <div class="main">
      <header class="topbar">
        <div>
          <div class="title">Recall Studio</div>
          <div class="muted">本地优先 · 手动同步 · 闭卷复习</div>
        </div>
        <div class="search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="7" />
            <path d="M20 20L16.5 16.5" />
          </svg>
          <input placeholder="搜索题目、标签、专题" />
        </div>
        <StatusPill status="手动同步" note="就绪" />
      </header>
      <div class="content">
        <RouterView />
      </div>
    </div>
    <ToastHost />
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import NavRail from '../components/NavRail.vue'
import StatusPill from '../components/StatusPill.vue'
import ToastHost from '../components/ToastHost.vue'
</script>
