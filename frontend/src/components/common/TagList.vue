<script setup lang="ts">
defineProps<{
  tags: string | null
}>()
</script>

<template>
  <span v-if="tags" class="tag-list">
    <span
      v-for="tag in tags.split(',').filter(Boolean)"
      :key="tag"
      class="tag-item"
    >
      {{ tag }}
    </span>
  </span>
  <span v-else class="no-tags">—</span>
</template>

<style scoped>
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  display: inline-block;
  padding: 1px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--text-label-sm);
  font-weight: 500;
  color: var(--color-secondary);
  background: var(--color-surface-container);
  white-space: nowrap;
}

.no-tags {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}
</style>
