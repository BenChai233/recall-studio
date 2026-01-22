<template>
  <aside class="nav-rail">
    <div class="brand">
      <div class="brand-title">Recall Studio</div>
      <div class="brand-sub">专注复习</div>
    </div>

    <nav class="nav-links">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        active-class="active"
      >
        <span v-html="item.icon" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="nav-footer">
      <button class="btn primary" type="button">
        <span>新建牌组</span>
      </button>
      <div class="pill">
        <span>数据目录</span>
        <span class="muted">../database</span>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

const navItems = [
  {
    label: '今日',
    path: '/',
    icon:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 6h14v12H5z"/><path d="M5 10h14"/><path d="M8 3v4M16 3v4"/></svg>',
  },
  {
    label: '题库',
    path: '/decks',
    icon:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 7h16v12H4z"/><path d="M4 7l8-4 8 4"/></svg>',
  },
  {
    label: '复习',
    path: '/review',
    icon:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 4h9l3 3v13H6z"/><path d="M9 9h6M9 13h6M9 17h4"/></svg>',
  },
  {
    label: '统计',
    path: '/insights',
    icon:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 19V5"/><path d="M8 19v-6"/><path d="M12 19V9"/><path d="M16 19v-4"/><path d="M20 19V7"/></svg>',
  },
  {
    label: '设置',
    path: '/settings',
    icon:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 6h16M4 12h16M4 18h16"/><circle cx="8" cy="6" r="2"/><circle cx="16" cy="12" r="2"/><circle cx="10" cy="18" r="2"/></svg>',
  },
]
</script>
