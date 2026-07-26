<script setup lang="ts">
defineProps<{
  message?: string
  icon?: string
}>()
</script>

<template>
  <div class="empty-state">
    <span v-if="icon" class="empty-icon">{{ icon }}</span>
    <p class="empty-message">{{ message || '暂无数据' }}</p>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  color: var(--color-outline-variant);
}

.empty-message {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  text-align: center;
}
</style>
