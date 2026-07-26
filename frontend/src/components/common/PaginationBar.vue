<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  total: number
  size: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
const pages = computed(() => {
  const current = props.page
  const last = totalPages.value
  const result: (number | '...')[] = []
  if (last <= 7) {
    for (let i = 1; i <= last; i++) result.push(i)
    return result
  }
  result.push(1)
  if (current > 3) result.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(last - 1, current + 1); i++) {
    result.push(i)
  }
  if (current < last - 2) result.push('...')
  result.push(last)
  return result
})
</script>

<template>
  <div v-if="totalPages > 1" class="pagination-bar">
    <button
      class="page-btn"
      :disabled="page <= 1"
      @click="emit('change', page - 1)"
    >
      上一页
    </button>
    <template v-for="p in pages" :key="p">
      <span v-if="p === '...'" class="page-ellipsis">...</span>
      <button
        v-else
        class="page-btn"
        :class="{ active: p === page }"
        @click="emit('change', p as number)"
      >
        {{ p }}
      </button>
    </template>
    <button
      class="page-btn"
      :disabled="page >= totalPages"
      @click="emit('change', page + 1)"
    >
      下一页
    </button>
    <span class="page-info">共 {{ total }} 条</span>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 16px 0;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: var(--color-surface-card);
  color: var(--color-on-surface);
  font-size: var(--text-body-md);
  cursor: pointer;
  transition: border-color 0.2s;
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: var(--color-primary-container);
}

.page-btn.active {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border-color: var(--color-primary-container);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-ellipsis {
  width: 36px;
  text-align: center;
  color: var(--color-on-surface-variant);
}

.page-info {
  margin-left: 12px;
  font-size: var(--text-body-md);
  color: var(--color-on-surface-variant);
}
</style>
