<script setup lang="ts">
import type { ProblemStatusView } from '@/types/problem'
import { actionLabels } from '@/constants/labels'

defineProps<{
  problemId: number
  status: ProblemStatusView
}>()

const emit = defineEmits<{
  edit: [id: number]
  deactivate: [id: number]
  restore: [id: number]
}>()
</script>

<template>
  <div class="action-btns">
    <button class="action-btn edit-action" @click.stop="emit('edit', problemId)">
      {{ actionLabels.edit }}
    </button>
    <button
      v-if="status === 'ACTIVE'"
      class="action-btn deactivate-action"
      @click.stop="emit('deactivate', problemId)"
    >
      {{ actionLabels.deactivate }}
    </button>
    <button
      v-else
      class="action-btn restore-action"
      @click.stop="emit('restore', problemId)"
    >
      {{ actionLabels.restore }}
    </button>
  </div>
</template>

<style scoped>
.action-btns {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 4px 14px;
  border-radius: var(--radius-md);
  font-size: var(--text-label-sm);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: 1px solid var(--color-border-subtle);
  background: transparent;
}

.action-btn:hover {
  opacity: 0.85;
}

.edit-action {
  color: var(--color-primary-container);
  border-color: var(--color-primary-container);
}

.deactivate-action {
  color: #e65100;
  border-color: #e65100;
}

.restore-action {
  color: var(--color-primary-container);
  border-color: var(--color-primary-container);
}
</style>
