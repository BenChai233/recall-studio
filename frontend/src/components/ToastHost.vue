<template>
  <div class="toast-host">
    <div v-for="toast in toastState.list" :key="toast.id" class="toast" :class="toast.type">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { useToast } from '../composables/toast'

const toastState = useToast()
</script>
