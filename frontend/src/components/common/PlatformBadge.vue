<script setup lang="ts">
import { platformLabels } from '@/constants/labels'

defineProps<{
  platform: string
}>()
</script>

<template>
  <span class="platform-badge">{{ platformLabels[platform] || platform }}</span>
</template>

<style scoped>
.platform-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
  background: var(--color-surface-container-high);
  white-space: nowrap;
}
</style>
