<script setup lang="ts">
import type { MineProblemStatusFilter } from '@/types/problem'
import { problemStatusFilterLabels } from '@/constants/labels'

defineProps<{
  modelValue: MineProblemStatusFilter
}>()

const emit = defineEmits<{
  'update:modelValue': [value: MineProblemStatusFilter]
}>()

const tabs: MineProblemStatusFilter[] = ['ALL', 'ACTIVE', 'INACTIVE']

function select(value: MineProblemStatusFilter) {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="status-tabs">
    <button
      v-for="tab in tabs"
      :key="tab"
      class="tab-btn"
      :class="{ 'tab-active': modelValue === tab }"
      @click="select(tab)"
    >
      {{ problemStatusFilterLabels[tab] }}
    </button>
  </div>
</template>

<style scoped>
.status-tabs {
  display: inline-flex;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-subtle);
}

.tab-btn {
  padding: 6px 16px;
  font-size: var(--text-body-md);
  font-weight: 500;
  color: var(--color-on-surface-variant);
  background: transparent;
  border: none;
  cursor: pointer;
  border-right: 1px solid var(--color-border-subtle);
  transition: background 0.15s, color 0.15s;
}

.tab-btn:last-child {
  border-right: none;
}

.tab-btn:hover {
  background: var(--color-surface-container-low);
}

.tab-active {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  font-weight: 600;
}

.tab-active:hover {
  background: var(--color-primary-container);
}
</style>
