<template>
  <div class="pill">
    <span>{{ status }}</span>
    <span class="muted">{{ note }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ status: string; note?: string }>()
</script>
