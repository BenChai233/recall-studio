<template>
  <section class="card">
    <h2 class="section-title">页面不存在</h2>
    <p class="muted">你访问的页面未找到。</p>
    <RouterLink class="btn" to="/">返回首页</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
