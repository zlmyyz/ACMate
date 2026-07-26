<script setup lang="ts">
defineProps<{
  message: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="error-state">
    <p class="error-text">{{ message }}</p>
    <button class="retry-btn" @click="emit('retry')">重试</button>
  </div>
</template>

<style scoped>
.error-state {
  text-align: center;
  padding: 48px 24px;
}

.error-text {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  margin-bottom: 16px;
}

.retry-btn {
  padding: 8px 28px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
}

.retry-btn:hover {
  opacity: 0.9;
}
</style>
