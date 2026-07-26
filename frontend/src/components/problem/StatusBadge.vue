<script setup lang="ts">
import { computed } from 'vue'
import type { ProblemStatusView } from '@/types/problem'
import { problemStatusLabels } from '@/constants/labels'

const props = defineProps<{
  status: ProblemStatusView
}>()

const label = computed(() => problemStatusLabels[props.status] || props.status)
const isActive = computed(() => props.status === 'ACTIVE')
</script>

<template>
  <span class="status-badge" :class="{ 'status-active': isActive, 'status-inactive': !isActive }">
    <span class="status-dot" :class="{ 'dot-active': isActive, 'dot-inactive': !isActive }"></span>
    {{ label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 12px;
  border-radius: 999px;
  font-size: var(--text-label-sm);
  font-weight: 600;
  white-space: nowrap;
}

.status-active {
  background: rgba(37, 187, 155, 0.12);
  color: #1a7d64;
}

.status-inactive {
  background: var(--color-surface-container-highest);
  color: var(--color-on-surface-variant);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-active {
  background: var(--color-primary-container);
}

.dot-inactive {
  background: #9499a0;
}
</style>
